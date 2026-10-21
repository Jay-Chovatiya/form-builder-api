package com.dynamicform.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormResponseDto {

    @NotNull(message = "Field ID is required")
    private Long fieldId;

    private Object value;
}
