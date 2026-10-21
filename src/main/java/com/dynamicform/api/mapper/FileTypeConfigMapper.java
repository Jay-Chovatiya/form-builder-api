package com.dynamicform.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.dto.FileTypeConfigDto;
import com.dynamicform.api.entity.FileTypeConfigEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileTypeConfigMapper extends BaseMapper<FileTypeConfigDto, FileTypeConfigEntity> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formField", ignore = true)
    void updateEntityFromDto(FileTypeConfigDto dto, @MappingTarget FileTypeConfigEntity entity);
}
