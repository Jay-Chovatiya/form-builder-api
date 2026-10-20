package com.dynamicform.api.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FormFieldDto;
import com.dynamicform.api.entity.FormFieldEntity;

@Mapper(componentModel = "spring", uses = {FieldOptionMapper.class, FieldValidationMapper.class, FileTypeConfigMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormFieldMapper extends BaseMapper<FormFieldDto, FormFieldEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formSection", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "validations", ignore = true)
    @Mapping(target = "allowedTypes", ignore = true)
    void updateEntityFromDto(FormFieldDto dto, @MappingTarget FormFieldEntity entity);

    @AfterMapping
    default void linkChildren(@MappingTarget FormFieldEntity field) {
        if (field.getOptions() != null) {
            field.getOptions().forEach(o -> o.setFormField(field));
        }
        if (field.getValidations() != null) {
            field.getValidations().forEach(v -> v.setFormField(field));
        }
        if (field.getAllowedTypes() != null) {
            field.getAllowedTypes().forEach(t -> t.setFormField(field));
        }
    }
}
