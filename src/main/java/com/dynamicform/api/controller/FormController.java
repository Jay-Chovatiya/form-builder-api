package com.dynamicform.api.controller;

import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.service.FormService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with id: " + id));

        return ResponseEntity.ok(ApiResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<FormDto>> getFormByCode(
            @PathVariable String code,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        FormDto form = formService.getFormByCode(code, includeDeleted)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with code: " + code));

        return ResponseEntity.ok(ApiResponse.ok(form, "Form retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Long id) {
        boolean deleted = formService.deleteForm(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Form not found with id: " + id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Form deleted successfully"));
    }
}
