package com.dynamicform.api.controller;

import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.FormSubmissionDto;
import com.dynamicform.api.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

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
