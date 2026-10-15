package com.dynamicform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileTypeConfigDto {

    @NotBlank(message = "File extension is required")
    @Size(max = 50, message = "Extension must not exceed 50 characters")
    private String extension;

    @NotBlank(message = "MIME type is required")
    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;
}
