package com.dynamicform.api.dto;

import com.dynamicform.api.entity.FormStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FormDto {

    private Long id;

    @NotBlank(message = "Form name is required")
    @Size(max = 200, message = "Form name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Form code is required")
    @Size(max = 100, message = "Form code must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Form code must contain only letters, numbers, hyphens, and underscores")
    private String code;

    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Form status is required")
    private FormStatus status = FormStatus.Draft;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean allowMultipleSubmissions;

    private boolean allowSaveAsDraft;

    private String confirmationMessage;

    @Size(max = 50, message = "Submit button text must not exceed 50 characters")
    private String submitButtonText;

    @Size(max = 50, message = "Cancel button text must not exceed 50 characters")
    private String cancelButtonText;

    @Size(max = 50, message = "Theme must not exceed 50 characters")
    private String theme;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 255, message = "Header text must not exceed 255 characters")
    private String headerText;

    @Size(max = 255, message = "Footer text must not exceed 255 characters")
    private String footerText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Valid
    private List<FormSectionDto> sections = new ArrayList<>();
}
