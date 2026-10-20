package com.dynamicform.api.service;

import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.entity.FormStatus;

import java.util.List;
import java.util.Optional;

public interface FormService {

    List<FormDto> getForms();

    Optional<FormDto> getFormWithDetails(Long id, boolean includeDeleted);

    Optional<FormDto> getFormWithDetails(Long id);

    Optional<FormDto> getFormByCode(String code, boolean includeDeleted);

    Optional<FormDto> getPublishedFormByCode(String code);

    boolean isFormCodeUnique(String code, Long excludeId);

    FormDto createForm(FormDto dto);

    Optional<FormDto> updateForm(Long id, FormDto dto);

    Optional<FormDto> updateFormStatus(Long id, FormStatus status);

    Optional<FormDto> publishForm(Long id);

    Optional<FormDto> unpublishForm(Long id);

    Optional<FormDto> duplicateForm(Long id);

    boolean deleteForm(Long id);
}
