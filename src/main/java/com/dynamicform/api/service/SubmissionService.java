package com.dynamicform.api.service;

import com.dynamicform.api.dto.FormSubmissionDto;

import java.util.List;
import java.util.Optional;

public interface SubmissionService {

    List<FormSubmissionDto> getSubmissions(Long formId);

    Optional<FormSubmissionDto> getSubmissionById(Long formId, Long submissionId);

    FormSubmissionDto saveSubmission(String code, FormSubmissionDto submissionDto);

    boolean deleteSubmission(Long formId, Long submissionId);
}
