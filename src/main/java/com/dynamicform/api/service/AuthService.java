package com.dynamicform.api.service;

import java.util.Optional;

import com.dynamicform.api.dto.AuthResponseDto;
import com.dynamicform.api.dto.LoginDto;

public interface AuthService {
    Optional<AuthResponseDto> login(LoginDto dto);
    Optional<AuthResponseDto> refreshToken(String refreshToken);
    boolean revokeToken(String refreshToken);
}
