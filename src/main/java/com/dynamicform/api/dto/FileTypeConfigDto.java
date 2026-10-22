package com.dynamicform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeConfigDto {

    @NotBlank(message = "File extension is required")
    @Size(max = 50, message = "Extension must not exceed 50 characters")
    @Pattern(
        regexp = "^\\.[a-zA-Z0-9]+$",
        message = "File extension must start with a dot followed by alphanumeric characters (e.g. '.pdf', '.png')"
    )
    private String extension;

    @NotBlank(message = "MIME type is required")
    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]*/[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]*$",
        message = "MIME type must be in a valid format (e.g. 'application/pdf', 'image/png')"
    )
    private String mimeType;
}
