package com.dynamicform.api.dto;

import java.time.LocalDateTime;

import com.dynamicform.api.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {

    private Long id;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters.")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Username must start with a letter and contain only alphanumeric characters and underscores.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email address format.")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters.")
    private String password;

    @NotBlank(message = "Full name is required.")
    @Size(max = 200, message = "Full name cannot exceed 200 characters.")
    private String fullName;

    @NotNull(message = "Role is required.")
    private Role role = Role.Administrator;

    @JsonProperty("isActive")
    private boolean isActive = true;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
