package com.dynamicform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationDto {

    @NotBlank(message = "Validation type is required")
    @Size(max = 50, message = "Validation type must not exceed 50 characters")
    private String type;

    @Size(max = 255, message = "Validation value must not exceed 255 characters")
    private String value;
}
