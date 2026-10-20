package com.dynamicform.api.mapper;

import org.mapstruct.AfterMapping;
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

    @AfterMapping
    default void normalize(@MappingTarget FileTypeConfigEntity entity) {
        if (entity.getExtension() != null) {
            entity.setExtension(normalizeExtension(entity.getExtension()));
        }
        if (entity.getMimeType() != null) {
            entity.setMimeType(entity.getMimeType().trim().toLowerCase());
        }
    }

    static String normalizeExtension(String ext) {
        if (ext == null || ext.isBlank()) return "";
        String clean = ext.trim().toLowerCase();
        return clean.startsWith(".") ? clean : "." + clean;
    }
}
