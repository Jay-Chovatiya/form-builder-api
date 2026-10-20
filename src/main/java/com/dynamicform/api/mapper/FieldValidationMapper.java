package com.dynamicform.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FieldValidationDto;
import com.dynamicform.api.entity.FieldValidationEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FieldValidationMapper extends BaseMapper<FieldValidationDto, FieldValidationEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formField", ignore = true)
    void updateEntityFromDto(FieldValidationDto dto, @MappingTarget FieldValidationEntity entity);
}
