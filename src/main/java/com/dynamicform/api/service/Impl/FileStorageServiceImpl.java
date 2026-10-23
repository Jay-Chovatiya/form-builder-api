package com.dynamicform.api.service.impl;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.entity.FormEntity;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.entity.FormStatus;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.service.FileStorageService;
import com.dynamicform.api.service.filevalidation.FileValidationPipeline;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FormRepository formRepository;
    private final FileValidationPipeline fileValidationPipeline;

    @Value("${formstudio.upload.dir}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public String uploadFieldFile(String formCode, String fieldName, MultipartFile file) {
        if (formCode == null || formCode.isBlank()) {
            throw new BadRequestException("Form code is required.");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new BadRequestException("Field name is required.");
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

        fileValidationPipeline.validate(file, field);

        String originalFilename = file.getOriginalFilename();
        String safeOriginalName = Paths.get(originalFilename).getFileName().toString();
        int dotIdx = safeOriginalName.lastIndexOf('.');
        String extension = dotIdx >= 0 ? safeOriginalName.substring(dotIdx).toLowerCase() : "";

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
}
