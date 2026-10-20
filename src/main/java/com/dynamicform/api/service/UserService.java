package com.dynamicform.api.service;

import java.util.List;
import java.util.Optional;

import com.dynamicform.api.dto.UserDto;

public interface UserService {

    List<UserDto> getUsers();

    Optional<UserDto> getUserById(Long id);

    UserDto createUser(UserDto dto);

    Optional<UserDto> updateUser(Long id, UserDto dto);

    boolean toggleUserStatus(Long id);

    boolean deleteUser(Long id);

    boolean isUsernameUnique(String username, Long excludeId);

    boolean isEmailUnique(String email, Long excludeId);
}
