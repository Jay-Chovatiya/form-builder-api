package com.dynamicform.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FormSectionDto {

    private Long id;

    @NotBlank(message = "Section title is required")
    @Size(max = 200, message = "Section title must not exceed 200 characters")
    private String title;

    private String description;

    @Size(max = 50, message = "Theme must not exceed 50 characters")
    private String theme;

    private boolean visibility;

    @Min(value = 0, message = "Display order must be zero or positive")
    private int displayOrder;

    private boolean isDeleted;

    @Valid
    private List<FormFieldDto> fields = new ArrayList<>();
}
