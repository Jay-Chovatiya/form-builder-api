package com.dynamicform.api.service.impl;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.entity.FormEntity;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.entity.FormStatus;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FormRepository formRepository;

    @Value("${formstudio.upload.dir:D:/Anguler/formstudio-storage/uploads}")
    private String uploadDir;

    private static final byte[] EXE_MZ_SIGNATURE = new byte[]{(byte) 0x4D, (byte) 0x5A};

    private static final Set<String> PLAIN_TEXT_EXTENSIONS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        PLAIN_TEXT_EXTENSIONS.addAll(List.of(".csv", ".txt", ".json", ".xml"));
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
    @Transactional(readOnly = true)
    public String uploadFieldFile(String formCode, String fieldName, MultipartFile file) {
        if (formCode == null || formCode.isBlank()) {
            throw new BadRequestException("Form code is required.");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new BadRequestException("Field name is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a valid non-empty file to upload.");
        }

        String trimmedCode = formCode.trim();
        String trimmedFieldName = fieldName.trim();

        FormEntity form = formRepository.findByCodeAndStatus(trimmedCode, FormStatus.Published)
                .orElseThrow(() -> new ResourceNotFoundException("Active form with code '" + trimmedCode + "' was not found."));

        LocalDate today = LocalDate.now();
        if (form.getStartDate() != null && today.isBefore(form.getStartDate())) {
            throw new BadRequestException("This form is not yet open for submissions.");
        }
        if (form.getEndDate() != null && today.isAfter(form.getEndDate())) {
            throw new BadRequestException("This form is closed for submissions.");
        }

        FormFieldEntity field = form.getSections().stream()
                .filter(s -> !s.isDeleted())
                .flatMap(s -> s.getFields().stream())
                .filter(f -> !f.isDeleted() && "File".equalsIgnoreCase(f.getType()) && trimmedFieldName.equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("File field '%s' was not found for active form '%s'.", trimmedFieldName, trimmedCode)));

        if (field.getAllowedTypes() == null || field.getAllowedTypes().isEmpty()) {
            throw new BadRequestException(String.format("File uploads are not permitted for field '%s' because no allowed types are configured.", field.getLabel()));
        }

        // Validate max size in bytes
        if (field.getMaxSizeInBytes() > 0 && file.getSize() > field.getMaxSizeInBytes()) {
            throw new BadRequestException(String.format(
                    "File size (%d bytes) exceeds the maximum allowed size of %d bytes for field '%s'.",
                    file.getSize(), field.getMaxSizeInBytes(), field.getLabel()));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File name is invalid.");
        }

        String safeOriginalName = Paths.get(originalFilename).getFileName().toString();
        int dotIdx = safeOriginalName.lastIndexOf('.');
        if (dotIdx < 0) {
            throw new BadRequestException("File must have a valid extension.");
        }
        String extension = safeOriginalName.substring(dotIdx).toLowerCase();

        // Validate extension matches allowedTypes
        Set<String> allowedExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        field.getAllowedTypes().forEach(at -> allowedExtensions.add(at.getExtension().trim()));

        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(String.format(
                    "File extension '%s' is not allowed for field '%s'. Allowed extensions: %s",
                    extension, field.getLabel(), allowedExtensions));
        }

        // Validate binary signature (magic bytes)
        validateFileSignature(file, extension);

        // Store file safely on disk
        String nameWithoutExt = dotIdx > 0 ? safeOriginalName.substring(0, dotIdx) : "file";
        String cleanBaseName = nameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");
        String uniqueFileName = cleanBaseName + "_" + UUID.randomUUID().toString().replace("-", "") + extension;

        Path targetDirectory = Paths.get(uploadDir, trimmedCode).toAbsolutePath().normalize();
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(targetDirectory);
            Path destinationPath = targetDirectory.resolve(uniqueFileName).normalize();

            if (!destinationPath.startsWith(targetDirectory)) {
                throw new BadRequestException("Invalid file destination path.");
            }

            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file: " + e.getMessage(), e);
        }

        return "/api/forms/" + trimmedCode + "/files/" + uniqueFileName;
    }

    @Override
    public Resource loadFileAsResource(String formCode, String fileName) {
        if (formCode == null || formCode.isBlank() || fileName == null || fileName.isBlank()) {
            throw new ResourceNotFoundException("File name and form code are required.");
        }

        String safeFileName = Paths.get(fileName).getFileName().toString();
        Path targetDirectory = Paths.get(uploadDir, formCode.trim()).toAbsolutePath().normalize();
        Path filePath = targetDirectory.resolve(safeFileName).normalize();

        if (!filePath.startsWith(targetDirectory)) {
            throw new BadRequestException("Invalid file path traversal attempt.");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResourceNotFoundException("The requested file was not found.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("The requested file is not readable.");
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("The requested file was not found.");
        }
    }

    @Override
    public String getFileContentType(String formCode, String fileName) {
        try {
            String safeFileName = Paths.get(fileName).getFileName().toString();
            Path filePath = Paths.get(uploadDir, formCode.trim(), safeFileName).toAbsolutePath().normalize();
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public boolean removeFile(String formCode, String fileName) {
        if (formCode == null || formCode.isBlank() || fileName == null || fileName.isBlank()) {
            return false;
        }

        String safeFileName = Paths.get(fileName).getFileName().toString();
        Path targetDirectory = Paths.get(uploadDir, formCode.trim()).toAbsolutePath().normalize();
        Path filePath = targetDirectory.resolve(safeFileName).normalize();

        if (!filePath.startsWith(targetDirectory)) {
            return false;
        }

        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    private void validateFileSignature(MultipartFile file, String extension) {
        byte[] headerBytes = new byte[16];
        int bytesRead;
        try (InputStream in = file.getInputStream()) {
            bytesRead = in.read(headerBytes, 0, headerBytes.length);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file content for validation.");
        }

        if (bytesRead < 2) {
            throw new BadRequestException("File is too small to contain a valid file signature.");
        }

        // Reject Windows executable / PE header (MZ)
        if (matchesSignature(headerBytes, EXE_MZ_SIGNATURE)) {
            throw new BadRequestException("Executable binary files (MZ header) are strictly prohibited.");
        }

        // Plain text validation: must not contain binary null bytes
        if (PLAIN_TEXT_EXTENSIONS.contains(extension)) {
            for (int i = 0; i < bytesRead; i++) {
                if (headerBytes[i] == 0x00) {
                    throw new BadRequestException(String.format("File content is not valid text/CSV (contains binary null bytes)."));
                }
            }
            return;
        }

        // Known binary signatures check
        List<byte[]> signatures = KNOWN_SIGNATURES.get(extension);
        if (signatures != null) {
            boolean matches = signatures.stream().anyMatch(sig -> matchesSignature(headerBytes, sig));
            if (!matches) {
                throw new BadRequestException(String.format(
                        "File content does not match the expected binary signature for extension '%s'.", extension));
            }
        }
    }

    private boolean matchesSignature(byte[] header, byte[] signature) {
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
