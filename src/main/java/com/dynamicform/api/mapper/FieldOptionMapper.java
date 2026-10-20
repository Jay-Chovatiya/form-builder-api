package com.dynamicform.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FieldOptionDto;
import com.dynamicform.api.entity.FieldOptionEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FieldOptionMapper extends BaseMapper<FieldOptionDto, FieldOptionEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formField", ignore = true)
    void updateEntityFromDto(FieldOptionDto dto, @MappingTarget FieldOptionEntity entity);
}
