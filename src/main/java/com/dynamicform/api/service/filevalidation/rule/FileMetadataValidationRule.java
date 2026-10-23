package com.dynamicform.api.service.filevalidation.rule;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

/**
 * Layer 1: Validates fundamental file metadata:
 * - Non-null and non-empty file payload
 * - Maximum file size check
 * - Filename sanity (null bytes, path traversal sequences, invalid characters)
 */
@Component
@Order(1)
public class FileMetadataValidationRule implements FileValidationRule {

    private static final String INVALID_FILENAME_CHARS = ":*?\"<>|";

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded or file is empty.");
        }

        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null || rawFileName.isBlank()) {
            throw new BadRequestException("Uploaded file must have a valid filename.");
        }

        if (rawFileName.indexOf('\0') >= 0) {
            throw new BadRequestException("Filename contains invalid null byte characters.");
        }

        if (rawFileName.contains("..") || rawFileName.contains("/") || rawFileName.contains("\\")) {
            throw new BadRequestException("Filename contains invalid path traversal characters.");
        }

        for (int i = 0; i < rawFileName.length(); i++) {
            char c = rawFileName.charAt(i);
            if (c < 32 || INVALID_FILENAME_CHARS.indexOf(c) >= 0) {
                throw new BadRequestException("Filename contains invalid system characters.");
            }
        }

        long maxSize = field.getMaxSizeInBytes() > 0 ? field.getMaxSizeInBytes() : 5242880L;
        if (file.getSize() > maxSize) {
            double maxMb = Math.round((double) maxSize / (1024 * 1024) * 10.0) / 10.0;
            String fileName = Paths.get(rawFileName).getFileName().toString();
            throw new BadRequestException(String.format("File '%s' exceeds the maximum allowed size of %s MB.", fileName, maxMb));
        }
    }
}
