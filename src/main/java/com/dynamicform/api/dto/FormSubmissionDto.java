package com.dynamicform.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionDto {

    private Long id;

    private Long formId;

    private LocalDateTime submittedAt;

    @Valid
    private List<FormResponseDto> responses = new ArrayList<>();
}
