package com.dynamicform.api.service.Impl;

import com.dynamicform.api.dto.*;
import com.dynamicform.api.mapper.FormMapper;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.service.FormService;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@AllArgsConstructor 
public class FormServiceImpl implements FormService {

    private final FormRepository formRepository;
    private final FormMapper formMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormWithDetails(Long id, boolean includeDeleted) {
        return formRepository.findById(id)
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, includeDeleted));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormWithDetails(Long id) {
        return getFormWithDetails(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDto> getFormByCode(String code, boolean includeDeleted) {
        return formRepository.findByCode(code)
                .map(formMapper::toDto)
                .map(dto -> processDetails(dto, includeDeleted));
    }



    private FormDto processDetails(FormDto dto, boolean includeDeleted) {
        if (dto == null || dto.getSections() == null) {
            return dto;
        }

        // 1. Filter deleted sections if includeDeleted is false
        if (!includeDeleted) {
            dto.getSections().removeIf(FormSectionDto::isDeleted);
        }

        // 2. Sort sections by displayOrder
        dto.getSections().sort(Comparator.comparingInt(FormSectionDto::getDisplayOrder));

        for (FormSectionDto section : dto.getSections()) {
            if (section.getFields() != null) {
                // 3. Filter deleted fields inside each section
                if (!includeDeleted) {
                    section.getFields().removeIf(FormFieldDto::isDeleted);
                }

                // 4. Sort fields by displayOrder
                section.getFields().sort(Comparator.comparingInt(FormFieldDto::getDisplayOrder));

                // 5. Sort options inside each field by displayOrder
                for (FormFieldDto field : section.getFields()) {
                    if (field.getOptions() != null) {
                        field.getOptions().sort(Comparator.comparingInt(FieldOptionDto::getDisplayOrder));
                    }
                }
            }
        }

        return dto;
    }
}
