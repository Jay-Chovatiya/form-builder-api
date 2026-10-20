package com.dynamicform.api.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.dto.UserDto;
import com.dynamicform.api.entity.UserEntity;
import com.dynamicform.api.mapper.UserMapper;
import com.dynamicform.api.repository.UserRepository;
import com.dynamicform.api.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto dto) {
        if (dto == null) {
            throw new BadRequestException("User data is required.");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating a new user.");
        }

        UserEntity entity = userMapper.toEntity(dto);
        entity.setId(null);
        entity.setUsername(dto.getUsername().trim());
        entity.setEmail(dto.getEmail().trim().toLowerCase());
        entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        entity.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : null);

        UserEntity savedEntity = userRepository.save(entity);
        return userMapper.toDto(savedEntity);
    }

    @Override
    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto dto) {
        if (dto == null) {
            throw new BadRequestException("User data is required.");
        }

        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        userMapper.updateEntityFromDto(dto, user);
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : null);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        UserEntity updated = userRepository.save(user);
        return Optional.of(userMapper.toDto(updated));
    }

    @Override
    @Transactional
    public boolean toggleUserStatus(Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        UserEntity user = userOpt.get();
        user.setActive(!user.isActive());
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        userRepository.delete(userOpt.get());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameUnique(String username, Long excludeId) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String cleanUsername = username.trim();
        if (excludeId != null && excludeId > 0) {
            return !userRepository.existsByUsernameIgnoreCaseAndIdNot(cleanUsername, excludeId);
        }
        return !userRepository.existsByUsernameIgnoreCase(cleanUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailUnique(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleanEmail = email.trim();
        if (excludeId != null && excludeId > 0) {
            return !userRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, excludeId);
        }
        return !userRepository.existsByEmailIgnoreCase(cleanEmail);
    }
}
