package com.dynamicform.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FormResponseDto;
import com.dynamicform.api.entity.FormResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormResponseMapper extends BaseMapper<FormResponseDto, FormResponseEntity> {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    @Mapping(target = "value", source = "valueJson")
    FormResponseDto toDto(FormResponseEntity entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formSubmission", ignore = true)
    @Mapping(target = "valueJson", expression = "java(mapValueToJson(dto.getValue()))")
    FormResponseEntity toEntity(FormResponseDto dto);

    default String mapValueToJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
