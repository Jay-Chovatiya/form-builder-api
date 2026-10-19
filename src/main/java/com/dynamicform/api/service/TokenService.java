package com.dynamicform.api.service;

import com.dynamicform.api.entity.UserEntity;

public interface TokenService {
    Long extractUserId(String token);
    String generateJWTToken(UserEntity user);
    boolean isTokenValid(String token, UserEntity user);
    String generateRefreshToken();
    String hashRefreshToken(String refreshToken);
}
