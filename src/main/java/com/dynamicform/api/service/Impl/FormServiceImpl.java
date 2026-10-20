package com.dynamicform.api.service.impl;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.dto.*;
import com.dynamicform.api.entity.*;
import com.dynamicform.api.mapper.*;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.repository.FormSubmissionRepository;
import com.dynamicform.api.service.FormService;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {

    private final FormRepository formRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FormMapper formMapper;
    private final FormSectionMapper formSectionMapper;
    private final FormFieldMapper formFieldMapper;
    private final FieldOptionMapper fieldOptionMapper;
    private final FieldValidationMapper fieldValidationMapper;
    private final FileTypeConfigMapper fileTypeConfigMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<FormDto> getForms() {
        return formRepository.findAllByOrderByIdDesc().stream()
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormWithDetails(Long id, boolean includeDeleted) {
        return formRepository.findById(id)
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, includeDeleted));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormWithDetails(Long id) {
        return getFormWithDetails(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormByCode(String code, boolean includeDeleted) {
        return formRepository.findByCode(code)
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, includeDeleted));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getPublishedFormByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return formRepository.findByCodeAndStatus(code.trim(), FormStatus.Published)
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, false));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFormCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String cleanCode = code.trim();
        if (excludeId != null && excludeId > 0) {
            return !formRepository.existsByCodeIgnoreCaseAndIdNot(cleanCode, excludeId);
        }
        return !formRepository.existsByCodeIgnoreCase(cleanCode);
    }

    @Override
    @Transactional
    public FormDto createForm(FormDto dto) {
        if (dto == null) {
            throw new BadRequestException("Form data is required.");
        }

        FormEntity entity = formMapper.toEntity(dto);
        entity.setId(null);
        if (entity.getStatus() == null) {
            entity.setStatus(FormStatus.Draft);
        }
        if (entity.getSections() != null) {
            for (FormSectionEntity section : entity.getSections()) {
                section.setId(null);
                if (section.getFields() != null) {
                    for (FormFieldEntity field : section.getFields()) {
                        field.setId(null);
                        if (field.getOptions() != null) {
                            field.getOptions().forEach(opt -> opt.setId(null));
                        }
                        if (field.getValidations() != null) {
                            field.getValidations().forEach(val -> val.setId(null));
                        }
                        if (field.getAllowedTypes() != null) {
                            field.getAllowedTypes().forEach(t -> t.setId(null));
                        }
                    }
                }
            }
        }

        FormEntity saved = formRepository.save(entity);
        return getFormWithDetails(saved.getId(), false).orElseThrow();
    }

    @Override
    @Transactional
    public Optional<FormDto> updateForm(Long id, FormDto dto) {
        if (dto == null) {
            throw new BadRequestException("Form data is required.");
        }

        Optional<FormEntity> existingOpt = formRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        FormEntity existing = existingOpt.get();
        formMapper.updateEntityFromDto(dto, existing);
        if (dto.getCode() != null) {
            existing.setCode(dto.getCode().trim());
        }

        synchronizeFormHierarchy(existing, dto);

        FormEntity saved = formRepository.save(existing);
        return getFormWithDetails(saved.getId(), false);
    }

    @Override
    @Transactional
    public boolean deleteForm(Long id) {
        Optional<FormEntity> entity = formRepository.findById(id);
        if (entity.isEmpty()) {
            return false;
        }

        boolean isSubmissionPresent = formSubmissionRepository.existsByFormId(id);
        if (isSubmissionPresent) {
            throw new BadRequestException("Cannot delete this form because it contains submissions. Please delete all form submissions before deleting the form.");
        }

        formRepository.delete(entity.get());
        return true;
    }

    @Override
    @Transactional
    public Optional<FormDto> updateFormStatus(Long id, FormStatus status) {
        if (status == null) {
            throw new BadRequestException("Status parameter is required.");
        }

        Optional<FormEntity> formOpt = formRepository.findById(id);
        if (formOpt.isEmpty()) {
            return Optional.empty();
        }

        FormEntity form = formOpt.get();
        form.setStatus(status);
        formRepository.save(form);

        return getFormWithDetails(id, false);
    }

    @Override
    @Transactional
    public Optional<FormDto> publishForm(Long id) {
        return updateFormStatus(id, FormStatus.Published);
    }

    @Override
    @Transactional
    public Optional<FormDto> unpublishForm(Long id) {
        return updateFormStatus(id, FormStatus.Unpublished);
    }

    @Override
    @Transactional
    public Optional<FormDto> duplicateForm(Long formId) {
        if (formId == null) {
            return Optional.empty();
        }

        Long newFormId = jdbcTemplate.execute(
                (CallableStatementCreator) connection -> {
                    CallableStatement statement = connection.prepareCall("call sp_duplicate_form(?, ?)");
                    statement.setInt(1, formId.intValue());
                    statement.setNull(2, Types.INTEGER);
                    statement.registerOutParameter(2, Types.INTEGER);
                    return statement;
                },
                (CallableStatementCallback<Long>) statement -> {
                    statement.execute();
                    int generatedId = statement.getInt(2);
                    return statement.wasNull() ? null : (long) generatedId;
                }
        );

        if (newFormId == null || newFormId <= 0) {
            return Optional.empty();
        }
        return getFormWithDetails(newFormId, false);
    }

    private FormDto processDetails(FormDto dto, boolean includeDeleted) {
        if (dto == null || dto.getSections() == null) {
            return dto;
        }

        if (!includeDeleted) {
            dto.getSections().removeIf(FormSectionDto::isDeleted);
        }

        dto.getSections().sort(Comparator.comparingInt(FormSectionDto::getDisplayOrder));

        for (FormSectionDto section : dto.getSections()) {
            if (section.getFields() != null) {
                if (!includeDeleted) {
                    section.getFields().removeIf(FormFieldDto::isDeleted);
                }

                section.getFields().sort(Comparator.comparingInt(FormFieldDto::getDisplayOrder));

                for (FormFieldDto field : section.getFields()) {
                    if (field.getOptions() != null) {
                        field.getOptions().sort(Comparator.comparingInt(FieldOptionDto::getDisplayOrder));
                    }
                }
            }
        }

        return dto;
    }

    private void synchronizeFormHierarchy(FormEntity existing, FormDto dto) {
        Set<Long> updatedSectionIds = dto.getSections() != null
                ? dto.getSections().stream()
                        .map(FormSectionDto::getId)
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        for (FormSectionEntity sec : existing.getSections()) {
            if (!updatedSectionIds.contains(sec.getId())) {
                sec.setDeleted(true);
                for (FormFieldEntity f : sec.getFields()) {
                    f.setDeleted(true);
                }
            }
        }

        if (dto.getSections() != null) {
            for (FormSectionDto sectionDto : dto.getSections()) {
                FormSectionEntity existingSec = null;
                if (sectionDto.getId() != null && sectionDto.getId() > 0) {
                    existingSec = existing.getSections().stream()
                            .filter(s -> s.getId() != null && s.getId().equals(sectionDto.getId()))
                            .findFirst().orElse(null);
                }

                if (existingSec != null) {
                    formSectionMapper.updateEntityFromDto(sectionDto, existingSec);
                    existingSec.setDeleted(false);
                    synchronizeFields(existingSec, sectionDto.getFields());
                } else {
                    FormSectionEntity newSection = formSectionMapper.toEntity(sectionDto);
                    newSection.setId(null);
                    newSection.setDeleted(false);
                    newSection.setForm(existing);
                    if (sectionDto.getFields() != null) {
                        newSection.getFields().clear();
                        for (FormFieldDto fieldDto : sectionDto.getFields()) {
                            newSection.getFields().add(createFieldEntity(fieldDto, newSection));
                        }
                    }
                    existing.getSections().add(newSection);
                }
            }
        }
    }

    private void synchronizeFields(FormSectionEntity existingSec, List<FormFieldDto> updatedFields) {
        Set<Long> updatedFieldIds = updatedFields != null
                ? updatedFields.stream()
                        .map(FormFieldDto::getId)
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        for (FormFieldEntity f : existingSec.getFields()) {
            if (!updatedFieldIds.contains(f.getId())) {
                f.setDeleted(true);
            }
        }

        if (updatedFields != null) {
            for (FormFieldDto fieldDto : updatedFields) {
                FormFieldEntity existingField = null;
                if (fieldDto.getId() != null && fieldDto.getId() > 0) {
                    existingField = existingSec.getFields().stream()
                            .filter(f -> f.getId() != null && f.getId().equals(fieldDto.getId()))
                            .findFirst().orElse(null);
                }

                if (existingField != null) {
                    formFieldMapper.updateEntityFromDto(fieldDto, existingField);
                    existingField.setDeleted(false);
                    synchronizeOptions(existingField, fieldDto.getOptions());
                    synchronizeValidations(existingField, fieldDto.getValidations());
                    synchronizeAllowedTypes(existingField, fieldDto.getAllowedTypes());
                } else {
                    FormFieldEntity newField = createFieldEntity(fieldDto, existingSec);
                    existingSec.getFields().add(newField);
                }
            }
        }
    }

    private FormFieldEntity createFieldEntity(FormFieldDto fieldDto, FormSectionEntity parentSection) {
        FormFieldEntity newField = formFieldMapper.toEntity(fieldDto);
        newField.setId(null);
        newField.setDeleted(false);
        newField.setFormSection(parentSection);
        if (newField.getOptions() != null) {
            newField.getOptions().forEach(o -> o.setId(null));
        }
        if (newField.getValidations() != null) {
            newField.getValidations().forEach(v -> v.setId(null));
        }
        if (newField.getAllowedTypes() != null) {
            newField.getAllowedTypes().forEach(t -> t.setId(null));
        }
        return newField;
    }

    private void synchronizeOptions(FormFieldEntity existingField, List<FieldOptionDto> updatedOptions) {
        Set<Long> updatedOptionIds = updatedOptions != null
                ? updatedOptions.stream()
                        .map(FieldOptionDto::getId)
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        existingField.getOptions().removeIf(o -> o.getId() != null && !updatedOptionIds.contains(o.getId()));

        if (updatedOptions != null) {
            for (FieldOptionDto oDto : updatedOptions) {
                FieldOptionEntity existingOpt = null;
                if (oDto.getId() != null && oDto.getId() > 0) {
                    existingOpt = existingField.getOptions().stream()
                            .filter(o -> o.getId() != null && o.getId().equals(oDto.getId()))
                            .findFirst().orElse(null);
                }

                if (existingOpt != null) {
                    fieldOptionMapper.updateEntityFromDto(oDto, existingOpt);
                } else {
                    FieldOptionEntity newOpt = fieldOptionMapper.toEntity(oDto);
                    newOpt.setId(null);
                    newOpt.setFormField(existingField);
                    existingField.getOptions().add(newOpt);
                }
            }
        }
    }

    private void synchronizeValidations(FormFieldEntity existingField, List<FieldValidationDto> updatedValidations) {
        Set<String> updatedTypes = updatedValidations != null
                ? updatedValidations.stream()
                        .map(FieldValidationDto::getType)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        existingField.getValidations().removeIf(v -> !updatedTypes.contains(v.getType()));

        if (updatedValidations != null) {
            for (FieldValidationDto vDto : updatedValidations) {
                FieldValidationEntity existingVal = existingField.getValidations().stream()
                        .filter(v -> Objects.equals(v.getType(), vDto.getType()))
                        .findFirst().orElse(null);

                if (existingVal != null) {
                    fieldValidationMapper.updateEntityFromDto(vDto, existingVal);
                } else {
                    FieldValidationEntity newVal = fieldValidationMapper.toEntity(vDto);
                    newVal.setId(null);
                    newVal.setFormField(existingField);
                    existingField.getValidations().add(newVal);
                }
            }
        }
    }

    private void synchronizeAllowedTypes(FormFieldEntity existingField, List<FileTypeConfigDto> updatedAllowedTypes) {
        Set<String> updatedExtensions = updatedAllowedTypes != null
                ? updatedAllowedTypes.stream()
                        .map(FileTypeConfigDto::getExtension)
                        .filter(Objects::nonNull)
                        .map(FileTypeConfigMapper::normalizeExtension)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        existingField.getAllowedTypes().removeIf(t -> !updatedExtensions.contains(FileTypeConfigMapper.normalizeExtension(t.getExtension())));

        if (updatedAllowedTypes != null) {
            for (FileTypeConfigDto tDto : updatedAllowedTypes) {
                String normExt = FileTypeConfigMapper.normalizeExtension(tDto.getExtension());
                FileTypeConfigEntity existingType = existingField.getAllowedTypes().stream()
                        .filter(t -> Objects.equals(t.getExtension(), normExt))
                        .findFirst().orElse(null);

                if (existingType != null) {
                    fileTypeConfigMapper.updateEntityFromDto(tDto, existingType);
                } else {
                    FileTypeConfigEntity newType = fileTypeConfigMapper.toEntity(tDto);
                    newType.setId(null);
                    newType.setFormField(existingField);
                    existingField.getAllowedTypes().add(newType);
                }
            }
        }
    }
}
