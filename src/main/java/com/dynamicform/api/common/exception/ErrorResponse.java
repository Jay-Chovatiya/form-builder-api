package com.dynamicform.api.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String message;
    private String detail;
    private Map<String, List<String>> errors;

    public static ErrorResponse of(int status, String message) {
        return of(status, message, message);
    }

    public static ErrorResponse of(int status, String message, String detail) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .detail(detail)
                .build();
    }

    public static ErrorResponse validation(Map<String, List<String>> errors) {
        return ErrorResponse.builder()
                .status(400)
                .message("Validation failed.")
                .detail("One or more validation errors occurred.")
                .errors(errors)
                .build();
    }
}
