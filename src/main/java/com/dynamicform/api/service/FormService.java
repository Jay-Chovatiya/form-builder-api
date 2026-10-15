package com.dynamicform.api.service;

import com.dynamicform.api.dto.FormDto;
import java.util.Optional;

public interface FormService {

    Optional<FormDto> getFormWithDetails(Long id, boolean includeDeleted);

    Optional<FormDto> getFormWithDetails(Long id);

    Optional<FormDto> getFormByCode(String code, boolean includeDeleted);
}
