package com.dynamicform.api.dto;

import java.time.LocalDateTime;

import com.dynamicform.api.entity.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    private Long id;

    private String username;

    private String email;

    private String fullName;

    private Role role;

    private boolean isActive;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
