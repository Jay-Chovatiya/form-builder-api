package com.dynamicform.api.service.filevalidation.rule;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

/**
 * Layer 4: Binary Magic Bytes & File Signature Validation:
 * - Inspects leading bytes (magic numbers) to verify the binary file signature
 * - Blocks disguised Windows executables/PE files (MZ header)
 * - Verifies known binary signatures for PDF, PNG, JPG, GIF, Office (DOCX/XLSX/PPTX, DOC/XLS/PPT), WebP, ZIP
 * - Enforces zero-null-byte integrity on plain text formats
 */
@Component
@Order(4)
public class FileSignatureValidationRule implements FileValidationRule {

    private static final byte[] EXE_MZ_SIGNATURE = new byte[]{(byte) 0x4D, (byte) 0x5A};

    private static final Set<String> PLAIN_TEXT_EXTENSIONS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        PLAIN_TEXT_EXTENSIONS.addAll(Arrays.asList(".csv", ".txt", ".json", ".xml"));
    }

    private static final Map<String, List<byte[]>> KNOWN_SIGNATURES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        // PDF: %PDF- (25 50 44 46)
        KNOWN_SIGNATURES.put(".pdf", List.of(new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46}));
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        KNOWN_SIGNATURES.put(".png", List.of(new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A}));
        // JPEG / JPG: FF D8 FF
        KNOWN_SIGNATURES.put(".jpg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        KNOWN_SIGNATURES.put(".jpeg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        // GIF: GIF87a or GIF89a
        KNOWN_SIGNATURES.put(".gif", List.of(
                new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x37, (byte) 0x61},
                new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61}
        ));
        // ZIP and modern Office (DOCX, XLSX, PPTX): PK.. (50 4B 03 04)
        KNOWN_SIGNATURES.put(".zip", List.of(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}));
        KNOWN_SIGNATURES.put(".docx", List.of(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}));
        KNOWN_SIGNATURES.put(".xlsx", List.of(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}));
        KNOWN_SIGNATURES.put(".pptx", List.of(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}));
        // Legacy Office Binary (.doc, .xls, .ppt): D0 CF 11 E0 A1 B1 1A E1
        KNOWN_SIGNATURES.put(".doc", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}));
        KNOWN_SIGNATURES.put(".xls", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}));
        KNOWN_SIGNATURES.put(".ppt", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}));
        // WebP: RIFF (52 49 46 46)
        KNOWN_SIGNATURES.put(".webp", List.of(new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46}));
    }

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString().trim();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";

        byte[] headerBytes = new byte[16];
        int bytesRead;
        try (InputStream in = file.getInputStream()) {
            bytesRead = in.read(headerBytes, 0, headerBytes.length);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file content for validation.");
        }

        if (bytesRead < 2) {
            throw new BadRequestException(String.format("File '%s' is too small to contain a valid file signature.", fileName));
        }

        if (matchesSignature(headerBytes, EXE_MZ_SIGNATURE)) {
            throw new BadRequestException(String.format(
                    "File '%s' is rejected because it contains executable binary content (MZ header).", fileName));
        }

        if (PLAIN_TEXT_EXTENSIONS.contains(extension)) {
            for (int i = 0; i < bytesRead; i++) {
                if (headerBytes[i] == 0x00) {
                    throw new BadRequestException(String.format(
                            "File '%s' is not a valid text/CSV file (contains binary null bytes).", fileName));
                }
            }
            return;
        }

        List<byte[]> validSignatures = KNOWN_SIGNATURES.get(extension);
        if (validSignatures != null) {
            boolean matches = validSignatures.stream().anyMatch(sig -> matchesSignature(headerBytes, sig));
            if (!matches) {
                throw new BadRequestException(String.format(
                        "File '%s' content does not match the expected binary signature for '%s'.", fileName, extension));
            }
        }
    }

    private static boolean matchesSignature(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
