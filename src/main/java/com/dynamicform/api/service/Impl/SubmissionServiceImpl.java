package com.dynamicform.api.service.impl;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.dto.FormResponseDto;
import com.dynamicform.api.dto.FormSubmissionDto;
import com.dynamicform.api.entity.*;
import com.dynamicform.api.mapper.FormSubmissionMapper;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.repository.FormSubmissionRepository;
import com.dynamicform.api.service.FileStorageService;
import com.dynamicform.api.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final FormRepository formRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSubmissionMapper formSubmissionMapper;
    private final FileStorageService fileStorageService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDto> getSubmissions(Long formId) {
        if (!formRepository.existsById(formId)) {
            throw new ResourceNotFoundException("Form with ID " + formId + " was not found.");
        }
        return formSubmissionRepository.findByFormIdOrderBySubmittedAtDesc(formId).stream()
                .map(formSubmissionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormSubmissionDto> getSubmissionById(Long formId, Long submissionId) {
        if (!formRepository.existsById(formId)) {
            throw new ResourceNotFoundException("Form with ID " + formId + " was not found.");
        }
        return formSubmissionRepository.findByIdAndFormId(submissionId, formId)
                .map(formSubmissionMapper::toDto);
    }

    @Override
    @Transactional
    public FormSubmissionDto saveSubmission(String code, FormSubmissionDto submissionDto) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Form code is required.");
        }
        if (submissionDto == null) {
            throw new BadRequestException("Submission payload cannot be empty.");
        }

        FormEntity form = formRepository.findByCodeAndStatus(code.trim(), FormStatus.Published)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid form URL or form is not currently active."));

        LocalDate now = LocalDate.now();
        if (form.getStartDate() != null && now.isBefore(form.getStartDate())) {
            throw new BadRequestException("This form will open for submissions on " + form.getStartDate().format(DATE_TIME_FORMATTER) + ".");
        }

        if (form.getEndDate() != null && now.isAfter(form.getEndDate())) {
            throw new BadRequestException("This form closed on " + form.getEndDate().format(DATE_TIME_FORMATTER) + ".");
        }

        if (!form.isAllowMultipleSubmissions() && formSubmissionRepository.existsByFormId(form.getId())) {
            throw new BadRequestException("This form does not allow multiple submissions.");
        }

        List<FormFieldEntity> allFields = form.getSections().stream()
                .filter(s -> !s.isDeleted())
                .flatMap(s -> s.getFields().stream())
                .filter(f -> !f.isDeleted())
                .toList();

        Map<Long, FormResponseDto> responseMap = submissionDto.getResponses() != null
                ? submissionDto.getResponses().stream()
                        .filter(r -> r.getFieldId() != null)
                        .collect(Collectors.toMap(FormResponseDto::getFieldId, r -> r, (r1, r2) -> r2))
                : Map.of();

        validateResponses(allFields, responseMap);

        FormSubmissionEntity submission = formSubmissionMapper.toEntity(submissionDto);
        submission.setId(null);
        submission.setForm(form);
        submission.setSubmittedAt(LocalDateTime.now());

        if (submission.getResponses() != null) {
            submission.getResponses().forEach(r -> {
                r.setId(null);
                r.setFormSubmission(submission);
            });
        }

        FormSubmissionEntity saved = formSubmissionRepository.save(submission);
        return formSubmissionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public boolean deleteSubmission(Long formId, Long submissionId) {
        if (!formRepository.existsById(formId)) {
            throw new ResourceNotFoundException("Form with ID " + formId + " was not found.");
        }

        FormSubmissionEntity submission = formSubmissionRepository.findByIdAndFormId(submissionId, formId)
                .orElse(null);
        if (submission == null) {
            return false;
        }

        // Clean up any uploaded files from disk for this submission
        if (submission.getForm() != null && submission.getResponses() != null) {
            String formCode = submission.getForm().getCode();
            Set<Long> fileFieldIds = submission.getForm().getSections().stream()
                    .flatMap(s -> s.getFields().stream())
                    .filter(f -> "File".equalsIgnoreCase(f.getType()))
                    .map(FormFieldEntity::getId)
                    .collect(Collectors.toSet());

            for (FormResponseEntity resp : submission.getResponses()) {
                if (fileFieldIds.contains(resp.getFieldId()) && resp.getValueJson() != null) {
                    List<String> fileUrls = parseFileUrls(resp.getValueJson());
                    for (String url : fileUrls) {
                        int lastSlash = url.lastIndexOf('/');
                        String fileName = lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
                        fileStorageService.removeFile(formCode, fileName);
                    }
                }
            }
        }

        formSubmissionRepository.delete(submission);
        return true;
    }

    private void validateResponses(List<FormFieldEntity> allFields, Map<Long, FormResponseDto> responseMap) {
        for (FormFieldEntity field : allFields) {
            FormResponseDto response = responseMap.get(field.getId());
            String responseStr = "";
            if (response != null && response.getValue() != null) {
                if (response.getValue() instanceof String s) {
                    responseStr = s.trim();
                } else {
                    responseStr = response.getValue().toString().trim();
                }
            }
            boolean hasValue = !responseStr.isEmpty();

            // 1. Required check
            boolean isRequired = field.getValidations() != null && field.getValidations().stream()
                    .anyMatch(v -> "required".equalsIgnoreCase(v.getType()));
            if (isRequired && !hasValue) {
                throw new BadRequestException(String.format("Field '%s' is required.", field.getLabel()));
            }

            if (!hasValue) {
                continue;
            }

            // 2. Validation constraints
            if (field.getValidations() != null) {
                for (FieldValidationEntity validation : field.getValidations()) {
                    if (validation.getType() == null) {
                        continue;
                    }
                    String valType = validation.getType().trim().toLowerCase();
                    String valConstraint = validation.getValue() != null ? validation.getValue().trim() : "";

                    switch (valType) {
                        case "minlength":
                            try {
                                int minLen = Integer.parseInt(valConstraint);
                                if (responseStr.length() < minLen) {
                                    throw new BadRequestException(String.format("Field '%s' must be at least %d characters.", field.getLabel(), minLen));
                                }
                            } catch (NumberFormatException ignored) {}
                            break;

                        case "maxlength":
                            try {
                                int maxLen = Integer.parseInt(valConstraint);
                                if (responseStr.length() > maxLen) {
                                    throw new BadRequestException(String.format("Field '%s' cannot exceed %d characters.", field.getLabel(), maxLen));
                                }
                            } catch (NumberFormatException ignored) {}
                            break;

                        case "minvalue":
                            try {
                                double minVal = Double.parseDouble(valConstraint);
                                double numVal = Double.parseDouble(responseStr);
                                if (numVal < minVal) {
                                    throw new BadRequestException(String.format("Field '%s' must be at least %s.", field.getLabel(), minVal));
                                }
                            } catch (NumberFormatException e) {
                                throw new BadRequestException(String.format("Field '%s' must be a valid number.", field.getLabel()));
                            }
                            break;

                        case "maxvalue":
                            try {
                                double maxVal = Double.parseDouble(valConstraint);
                                double numVal = Double.parseDouble(responseStr);
                                if (numVal > maxVal) {
                                    throw new BadRequestException(String.format("Field '%s' cannot exceed %s.", field.getLabel(), maxVal));
                                }
                            } catch (NumberFormatException e) {
                                throw new BadRequestException(String.format("Field '%s' must be a valid number.", field.getLabel()));
                            }
                            break;

                        case "email":
                            if (!EMAIL_PATTERN.matcher(responseStr).matches()) {
                                throw new BadRequestException(String.format("Field '%s' must be a valid email address.", field.getLabel()));
                            }
                            break;

                        case "pattern":
                            if (!valConstraint.isEmpty()) {
                                try {
                                    if (!Pattern.compile(valConstraint).matcher(responseStr).matches()) {
                                        throw new BadRequestException(String.format("Field '%s' format is invalid.", field.getLabel()));
                                    }
                                } catch (PatternSyntaxException ignored) {}
                            }
                            break;

                        default:
                            break;
                    }
                }
            }

            // 3. Choice options validation
            if ("Dropdown".equalsIgnoreCase(field.getType()) || "RadioButton".equalsIgnoreCase(field.getType())) {
                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    Set<String> allowedOptions = field.getOptions().stream()
                            .map(FieldOptionEntity::getValue)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                    if (!allowedOptions.isEmpty() && !allowedOptions.contains(responseStr)) {
                        throw new BadRequestException(String.format("Selected option '%s' for '%s' is invalid.", responseStr, field.getLabel()));
                    }
                }
            }

            // 4. File field restriction validation
            if ("File".equalsIgnoreCase(field.getType())) {
                if (field.getAllowedTypes() == null || field.getAllowedTypes().isEmpty()) {
                    throw new BadRequestException(String.format("Field '%s' does not permit file uploads because no allowed types are configured.", field.getLabel()));
                }

                Set<String> allowedExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                field.getAllowedTypes().forEach(at -> allowedExtensions.add(at.getExtension().trim()));

                List<String> fileUrls = parseFileUrls(responseStr);
                for (String fileUrl : fileUrls) {
                    int dot = fileUrl.lastIndexOf('.');
                    String fileExt = dot >= 0 ? fileUrl.substring(dot).toLowerCase() : "";
                    if (!allowedExtensions.contains(fileExt)) {
                        throw new BadRequestException(String.format("Uploaded file format '%s' for '%s' is not allowed.", fileExt, field.getLabel()));
                    }
                }
            }
        }
    }

    private static List<String> parseFileUrls(String jsonOrUrl) {
        if (jsonOrUrl == null || jsonOrUrl.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = jsonOrUrl.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
                List<String> urls = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode elem : node) {
                    if (elem.isTextual()) {
                        urls.add(elem.asText());
                    } else if (elem.has("fileUrl")) {
                        urls.add(elem.get("fileUrl").asText());
                    }
                }
                return urls;
            } catch (Exception e) {
                return List.of(trimmed);
            }
        }
        return List.of(trimmed);
    }
}
