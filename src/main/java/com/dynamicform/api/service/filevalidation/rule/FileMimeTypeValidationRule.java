package com.dynamicform.api.service.filevalidation.rule;

import com.dynamicform.api.common.constant.FileSecurityConstants;
import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FileTypeConfigEntity;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Layer 3: MIME Type & HTTP Content-Type Header Whitelist Validation:
 * - Ensures Content-Type header is present and well-formed (type/subtype syntax)
 * - Blocks dangerous or generic executable/script MIME types
 * - Strictly verifies that Content-Type matches the registered allowed MIME types for the file extension
 */
@Component
@Order(3)
public class FileMimeTypeValidationRule implements FileValidationRule {

    private static final Pattern MIME_TYPE_STRUCTURE_REGEX = Pattern.compile(
            "^[a-zA-Z0-9]+[a-zA-Z0-9!#$&^_.+-]*/[a-zA-Z0-9!#$&^_.+-]+$");

    private static final Set<String> DANGEROUS_MIME_TYPES = FileSecurityConstants.DANGEROUS_MIME_TYPES;

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        String rawContentType = file.getContentType() != null ? file.getContentType().trim().toLowerCase() : "";

        if (rawContentType.isBlank()) {
            throw new BadRequestException("Uploaded file is missing the Content-Type header.");
        }

        if (!MIME_TYPE_STRUCTURE_REGEX.matcher(rawContentType).matches()) {
            throw new BadRequestException(String.format("Invalid Content-Type header format: '%s'.", file.getContentType()));
        }

        if (DANGEROUS_MIME_TYPES.contains(rawContentType)) {
            throw new BadRequestException(String.format("Content-Type '%s' is strictly prohibited for security reasons.", file.getContentType()));
        }

        String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString().trim();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";

        FileTypeConfigEntity matchingConfig = null;
        if (field.getAllowedTypes() != null) {
            for (FileTypeConfigEntity t : field.getAllowedTypes()) {
                if (t.getExtension() != null && t.getExtension().trim().equalsIgnoreCase(extension)) {
                    matchingConfig = t;
                    break;
                }
            }
        }

        String expectedMimeType = (matchingConfig != null && matchingConfig.getMimeType() != null)
                ? matchingConfig.getMimeType().trim().toLowerCase()
                : null;

        if (expectedMimeType != null && !expectedMimeType.isBlank() && !expectedMimeType.equalsIgnoreCase(rawContentType)) {
            throw new BadRequestException(String.format(
                    "File '%s' has a Content-Type '%s' that does not match the allowed MIME type for '%s'. Expected: %s",
                    fileName, file.getContentType(), extension, expectedMimeType));
        }
    }
}
