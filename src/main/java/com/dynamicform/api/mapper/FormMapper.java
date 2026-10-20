package com.dynamicform.api.mapper;

import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.entity.FormEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {FormSectionMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormMapper extends BaseMapper<FormDto, FormEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(FormDto dto, @MappingTarget FormEntity entity);

    @AfterMapping
    default void linkChildren(@MappingTarget FormEntity form) {
        if (form.getSections() != null) {
            form.getSections().forEach(s -> s.setForm(form));
        }
    }
}
