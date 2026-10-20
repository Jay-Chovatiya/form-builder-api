package com.dynamicform.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.common.exception.ResourceNotFoundException;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.dto.UserDto;
import com.dynamicform.api.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers() {
        List<UserDto> users = userService.getUsers();
        return ResponseEntity.ok(ApiResponse.ok(users, "Users retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " was not found."));
        return ResponseEntity.ok(ApiResponse.ok(user, "User retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserDto dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating a new user.");
        }

        if (!userService.isUsernameUnique(dto.getUsername(), null)) {
            throw new BadRequestException("Username '" + dto.getUsername().trim() + "' is already taken.");
        }

        if (!userService.isEmailUnique(dto.getEmail(), null)) {
            throw new BadRequestException("Email '" + dto.getEmail().trim() + "' is already registered.");
        }

        UserDto createdUser = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "User created successfully", createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto dto) {

        if (!userService.isUsernameUnique(dto.getUsername(), id)) {
            throw new BadRequestException("Username '" + dto.getUsername().trim() + "' is already taken.");
        }

        if (!userService.isEmailUnique(dto.getEmail(), id)) {
            throw new BadRequestException("Email '" + dto.getEmail().trim() + "' is already registered.");
        }

        UserDto updatedUser = userService.updateUser(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " was not found."));

        return ResponseEntity.ok(ApiResponse.ok(updatedUser, "User updated successfully"));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        boolean success = userService.toggleUserStatus(id);
        if (!success) {
            throw new ResourceNotFoundException("User with ID " + id + " was not found.");
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "User status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        if (!success) {
            throw new ResourceNotFoundException("User with ID " + id + " was not found.");
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted successfully"));
    }
}
