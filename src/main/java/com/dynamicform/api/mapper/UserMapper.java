package com.dynamicform.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.dynamicform.api.common.mapper.BaseMapper;
import com.dynamicform.api.dto.UserDto;
import com.dynamicform.api.entity.UserEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends BaseMapper<UserDto, UserEntity> {
}
