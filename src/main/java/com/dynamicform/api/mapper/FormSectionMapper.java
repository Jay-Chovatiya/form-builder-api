package com.dynamicform.api.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FormSectionDto;
import com.dynamicform.api.entity.FormSectionEntity;

@Mapper(componentModel = "spring", uses = {FormFieldMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormSectionMapper extends BaseMapper<FormSectionDto, FormSectionEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "form", ignore = true)
    @Mapping(target = "fields", ignore = true)
    void updateEntityFromDto(FormSectionDto dto, @MappingTarget FormSectionEntity entity);

    @AfterMapping
    default void linkChildren(@MappingTarget FormSectionEntity section) {
        if (section.getFields() != null) {
            section.getFields().forEach(f -> f.setFormSection(section));
        }
    }
}
