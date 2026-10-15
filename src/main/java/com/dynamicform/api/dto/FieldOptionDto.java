package com.dynamicform.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FieldOptionDto {

    private Long id;

    @NotBlank(message = "Option label is required")
    @Size(max = 200, message = "Option label must not exceed 200 characters")
    private String label;

    @NotBlank(message = "Option value is required")
    @Size(max = 200, message = "Option value must not exceed 200 characters")
    private String value;

    @Min(value = 0, message = "Display order must be zero or positive")
    private int displayOrder;
}
