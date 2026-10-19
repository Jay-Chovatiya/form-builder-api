package com.dynamicform.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDto {
    @NotBlank(message = "Username or Email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe;
}
