package com.dynamicform.api.dto;

import com.dynamicform.api.common.validation.FormFieldValid;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@FormFieldValid 
public class FormFieldDto {

    private Long id;

    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message="Field name must start with a letter and contain only letters, numbers, or underscores.")
    private String name;

    @NotBlank(message = "Field type is required")
    @Size(max = 50, message = "Field type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Field label is required")
    @Size(max = 200, message = "Field label must not exceed 200 characters")
    private String label;

    private String helperDescription;

    private boolean visibility;

    @Size(max = 255, message = "Placeholder must not exceed 255 characters")
    private String placeholder;

    @JsonProperty("default")
    private String defaultValue;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @Min(value = 0, message = "Display order must be zero or positive")
    private int displayOrder;

    private boolean multiple;

    @Min(value = 1, message = "Max files must be at least 1")
    private Integer maxFiles;

    @Min(value = 0, message = "Max size in bytes must be zero or positive")
    private long maxSizeInBytes;

    @JsonProperty("isDeleted")
    private boolean isDeleted;

    @Valid
    private List<FileTypeConfigDto> allowedTypes = new ArrayList<>();

    @Valid
    private List<FieldOptionDto> options = new ArrayList<>();

    @Valid
    private List<FieldValidationDto> validations = new ArrayList<>();
}
