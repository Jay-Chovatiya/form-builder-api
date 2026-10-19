package com.dynamicform.api.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @Builder.Default
    private int statusCode = 200;


    private String message;

    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operation successful");
    }

    public static <T> ApiResponse<T> success(String message) {
        return ok(null, message);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> of(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
