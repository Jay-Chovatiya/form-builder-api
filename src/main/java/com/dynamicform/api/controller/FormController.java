package com.dynamicform.api.controller;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.FieldOptionDto;
import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.dto.FormFieldDto;
import com.dynamicform.api.dto.FormSectionDto;
import com.dynamicform.api.entity.FormStatus;
import com.dynamicform.api.service.FormService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController 
@RequestMapping("/api/forms")
@RequiredArgsConstructor 
public class FormController {

    private final FormService formService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FormDto>>> getForms() {
        List<FormDto> forms = formService.getForms();
        return ResponseEntity.ok(ApiResponse.ok(forms, "Forms retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FormDto>> getFormById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        FormDto form = formService.getFormWithDetails(id, includeDeleted)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<FormDto>> getPublishedFormByCode(@PathVariable String code) {
        FormDto form = formService.getPublishedFormByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid form URL or form is not currently active."));

        return ResponseEntity.ok(ApiResponse.ok(form, "Form retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FormDto>> createForm(@Valid @RequestBody FormDto dto) {
        if (dto == null) {
            throw new BadRequestException("Form data is required.");
        }

        validateStructureUniqueness(dto);

        if (!dto.getCode().isBlank() && !formService.isFormCodeUnique(dto.getCode(), null)) {
            throw new BadRequestException("Form code '" + dto.getCode().trim() + "' is already in use.");
        }

        FormDto createdForm = formService.createForm(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Form created successfully", createdForm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FormDto>> updateForm(
            @PathVariable Long id,
            @Valid @RequestBody FormDto dto) {

        if (dto == null) {
            throw new BadRequestException("Form data is required.");
        }

        validateStructureUniqueness(dto);

        if (!dto.getCode().isBlank() && !formService.isFormCodeUnique(dto.getCode(), id)) {
            throw new BadRequestException("Form code '" + dto.getCode().trim() + "' is already in use.");
        }

        FormDto updatedForm = formService.updateForm(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(updatedForm, "Form updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Long id) {
        boolean deleted = formService.deleteForm(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Form with ID " + id + " was not found.");
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Form deleted successfully"));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponse<FormDto>> updateFormStatus(
            @PathVariable Long id,
            @PathVariable String status) {

        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status parameter is required.");
        }

        FormStatus formStatus;
        try {
            formStatus = FormStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status '" + status + "'. Allowed statuses are: Draft, Published, Unpublished, Archived.");
        }

        FormDto updatedForm = formService.updateFormStatus(id, formStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(updatedForm, "Form status updated successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<FormDto>> publishForm(@PathVariable Long id) {
        FormDto publishedForm = formService.publishForm(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(publishedForm, "Form published successfully"));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<FormDto>> unpublishForm(@PathVariable Long id) {
        FormDto unpublishedForm = formService.unpublishForm(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(unpublishedForm, "Form unpublished successfully"));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<FormDto>> duplicateForm(@PathVariable Long id) {
        FormDto duplicatedForm = formService.duplicateForm(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(duplicatedForm, "Form duplicated successfully"));
    }

    private void validateStructureUniqueness(FormDto dto) {
        if (dto.getSections() == null) {
            return;
        }

        Set<String> sectionTitles = new HashSet<>();
        for (FormSectionDto s : dto.getSections()) {
            if (s.getTitle() != null && !s.getTitle().isBlank()) {
                String cleanTitle = s.getTitle().trim().toLowerCase();
                if (!sectionTitles.add(cleanTitle)) {
                    throw new BadRequestException("Section title '" + s.getTitle().trim() + "' must be unique within the form.");
                }
            }
        }

        Set<String> fieldNames = new HashSet<>();
        for (FormSectionDto s : dto.getSections()) {
            if (s.getFields() != null) {
                for (FormFieldDto f : s.getFields()) {
                    if (f.getName() != null && !f.getName().isBlank()) {
                        String cleanName = f.getName().trim().toLowerCase();
                        if (!fieldNames.add(cleanName)) {
                            throw new BadRequestException("Field name '" + f.getName().trim() + "' must be unique across the form.");
                        }
                    }

                    if (f.getOptions() != null) {
                        Set<String> optionValues = new HashSet<>();
                        for (FieldOptionDto o : f.getOptions()) {
                            if (o.getValue() != null && !o.getValue().isBlank()) {
                                String cleanVal = o.getValue().trim().toLowerCase();
                                if (!optionValues.add(cleanVal)) {
                                    throw new BadRequestException("Option value '" + o.getValue().trim() + "' must be unique within field '" + f.getName() + "'.");
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
