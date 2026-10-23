package com.dynamicform.api.service.filevalidation.rule;

import com.dynamicform.api.common.constant.FileSecurityConstants;
import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

/**
 * Layer 2: Extension & Anti-Double-Extension Whitelist Validation:
 * - Rejects extensionless files
 * - Whitelisting against the field's AllowedTypes (.pdf, .png, etc.)
 * - Blocks dangerous executable/script extensions
 * - Blocks double-extension attacks (e.g. invoice.php.pdf or report.exe.png)
 */
@Component
@Order(2)
public class FileExtensionValidationRule implements FileValidationRule {

    private static final Set<String> DANGEROUS_EXTENSIONS = FileSecurityConstants.DANGEROUS_EXTENSIONS;

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString().trim();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";

        if (extension.isBlank()) {
            throw new BadRequestException(String.format("File '%s' has no file extension.", fileName));
        }

        if (field.getAllowedTypes() == null || field.getAllowedTypes().isEmpty()) {
            throw new BadRequestException(String.format(
                    "File uploads are not permitted for field '%s' because no allowed types are configured.",
                    field.getLabel()));
        }

        Set<String> allowedExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        field.getAllowedTypes().forEach(t -> {
            if (t.getExtension() != null && !t.getExtension().isBlank()) {
                allowedExtensions.add(t.getExtension().trim());
            }
        });

        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(String.format(
                    "File '%s' has an unsupported extension '%s'. Allowed extensions: %s",
                    fileName, extension, String.join(", ", allowedExtensions)));
        }

        // Double extension detection (e.g. report.php.pdf)
        String[] tokens = fileName.split("\\.");
        if (tokens.length > 2) {
            for (int i = 1; i < tokens.length - 1; i++) {
                String candidateExt = "." + tokens[i].trim().toLowerCase();
                if (DANGEROUS_EXTENSIONS.contains(candidateExt)) {
                    throw new BadRequestException(String.format(
                            "File '%s' is rejected due to suspicious double extension containing executable type '%s'.",
                            fileName, candidateExt));
                }
            }
        }

        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(String.format(
                    "File extension '%s' is strictly prohibited for security reasons.", extension));
        }
    }
}
