package com.dynamicform.api.controller;

import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.FormSubmissionDto;
import com.dynamicform.api.service.FileStorageService;
import com.dynamicform.api.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{formId}/submissions")
    public ResponseEntity<ApiResponse<List<FormSubmissionDto>>> getSubmissions(@PathVariable Long formId) {
        List<FormSubmissionDto> submissions = submissionService.getSubmissions(formId);
        return ResponseEntity.ok(ApiResponse.ok(submissions, "Submissions retrieved successfully"));
    }


    @GetMapping("/{formId}/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<FormSubmissionDto>> getSubmissionById(
            @PathVariable Long formId,
            @PathVariable Long submissionId) {
        FormSubmissionDto submission = submissionService.getSubmissionById(formId, submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission with ID " + submissionId + " was not found for form ID " + formId + "."));
        return ResponseEntity.ok(ApiResponse.ok(submission, "Submission retrieved successfully"));
    }

    @PostMapping("/code/{code}/submissions")
    public ResponseEntity<ApiResponse<FormSubmissionDto>> submitForm(
            @PathVariable String code,
            @Valid @RequestBody FormSubmissionDto dto) {
        FormSubmissionDto created = submissionService.saveSubmission(code, dto);
        return new ResponseEntity<>(
                ApiResponse.of(HttpStatus.CREATED.value(), "Form submitted successfully", created),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{code}/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @PathVariable String code,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fieldName") String fieldName) {
        String fileUrl = fileStorageService.uploadFieldFile(code, fieldName, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("fileUrl", fileUrl), "File uploaded successfully"));
    }

    @GetMapping("/{code}/files/{fileName}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String code,
            @PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(code, fileName);
        String contentType = fileStorageService.getFileContentType(code, fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{formId}/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubmission(
            @PathVariable Long formId,
            @PathVariable Long submissionId) {
        boolean deleted = submissionService.deleteSubmission(formId, submissionId);
        if (!deleted) {
            throw new ResourceNotFoundException(
                    "Submission with ID " + submissionId + " was not found for form ID " + formId + ".");
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Submission deleted successfully"));
    }
}
