package com.dynamicform.api.controller;

import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.service.FormService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
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
}
