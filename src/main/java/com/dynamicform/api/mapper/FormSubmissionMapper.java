package com.dynamicform.api.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FormSubmissionDto;
import com.dynamicform.api.entity.FormSubmissionEntity;

@Mapper(componentModel = "spring", uses = {FormResponseMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormSubmissionMapper extends BaseMapper<FormSubmissionDto, FormSubmissionEntity> {

    @Override
    @Mapping(target = "formId", source = "form.id")
    FormSubmissionDto toDto(FormSubmissionEntity entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "form", ignore = true)
    FormSubmissionEntity toEntity(FormSubmissionDto dto);

    @AfterMapping
    default void linkChildren(@MappingTarget FormSubmissionEntity submission) {
        if (submission.getResponses() != null) {
            submission.getResponses().forEach(resp -> resp.setFormSubmission(submission));
        }
    }
}
