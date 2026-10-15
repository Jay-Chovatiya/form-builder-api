package com.dynamicform.api.mapper;

import com.dynamicform.api.dto.*;
import com.dynamicform.api.entity.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormMapper {

    FormDto toDto(FormEntity entity);

    List<FormDto> toDtoList(List<FormEntity> entities);

    FormSectionDto toDto(FormSectionEntity entity);

    FormFieldDto toDto(FormFieldEntity entity);

    FieldOptionDto toDto(FieldOptionEntity entity);

    FieldValidationDto toDto(FieldValidationEntity entity);

    FileTypeConfigDto toDto(FileTypeConfigEntity entity);
}
