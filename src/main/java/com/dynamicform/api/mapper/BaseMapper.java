package com.dynamicform.api.mapper;

import java.util.List;
import org.mapstruct.MappingTarget;

public interface BaseMapper<D, E> {

    D toDto(E entity);

    E toEntity(D dto);

    List<D> toDto(List<E> entityList);

    List<E> toEntity(List<D> dtoList);

    void updateEntityFromDto(D dto, @MappingTarget E entity);
}
