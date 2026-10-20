package com.dynamicform.api.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicform.api.config.JwtProperties;
import com.dynamicform.api.dto.AuthResponseDto;
import com.dynamicform.api.dto.LoginDto;
import com.dynamicform.api.dto.UserDto;
import com.dynamicform.api.entity.RefreshTokenEntity;
import com.dynamicform.api.entity.UserEntity;
import com.dynamicform.api.mapper.UserMapper;
import com.dynamicform.api.repository.RefreshTokenRepository;
import com.dynamicform.api.repository.UserRepository;
import com.dynamicform.api.service.AuthService;
import com.dynamicform.api.service.TokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder bPasswordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Optional<AuthResponseDto> login(LoginDto dto) {
        String identifier = dto.getUsernameOrEmail() != null ? dto.getUsernameOrEmail().trim() : "";
        UserEntity user = userRepository.findByUsernameOrEmail(identifier).orElse(null);
        if (user == null || !user.isActive()) {
            return Optional.empty();
        }

        boolean isPasswordValid = bPasswordEncoder.matches(dto.getPassword(), user.getPasswordHash());
        if (!isPasswordValid) {
            return Optional.empty();
        }

        String jwtToken = tokenService.generateJWTToken(user);
        String refreshToken = tokenService.generateRefreshToken();
        String hashedRefreshToken = tokenService.hashRefreshToken(refreshToken);

        long refreshDurationMs = dto.isRememberMe()
                ? jwtProperties.getRefreshExpirationMs() * 7
                : jwtProperties.getRefreshExpirationMs();

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(hashedRefreshToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshDurationMs)))
                .build();
        refreshTokenRepository.save(entity);

        LocalDateTime accessTokenExpiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(jwtProperties.getExpirationMs()));

        UserDto userDto = userMapper.toDto(user);

        AuthResponseDto responseDto =  AuthResponseDto.builder()
                .expiresAt(accessTokenExpiresAt)
                .user(userDto)
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();

        return Optional.of(responseDto);
    }

    @Override
    @Transactional
    public Optional<AuthResponseDto> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        String hashRefreshToken = tokenService.hashRefreshToken(refreshToken.trim());

        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHash(hashRefreshToken)
                .orElse(null);

        if (tokenEntity == null || !tokenEntity.isActive()) {
            return Optional.empty();
        }

        UserEntity user = tokenEntity.getUser();
        if (user == null || !user.isActive()) {
            return Optional.empty();
        }

        String newRefreshToken = tokenService.generateRefreshToken();
        String newHashRefreshToken = tokenService.hashRefreshToken(newRefreshToken);

        RefreshTokenEntity newRefreshTokenEntity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(newHashRefreshToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(tokenEntity.getExpiresAt())
                .build();

        newRefreshTokenEntity = refreshTokenRepository.save(newRefreshTokenEntity);

        tokenEntity.setRevokedAt(LocalDateTime.now());
        tokenEntity.setReplacedByTokenId(newRefreshTokenEntity.getId());
        refreshTokenRepository.save(tokenEntity);

        LocalDateTime accessTokenExpiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(jwtProperties.getExpirationMs()));

        String jwtToken = tokenService.generateJWTToken(user);
        UserDto userDto = userMapper.toDto(user);

        AuthResponseDto responseDto = AuthResponseDto.builder()
                .expiresAt(accessTokenExpiresAt)
                .user(userDto)
                .token(jwtToken)
                .refreshToken(newRefreshToken)
                .build();

        return Optional.of(responseDto);
    }

    @Override
    @Transactional
    public boolean revokeToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        String hashRefreshToken = tokenService.hashRefreshToken(refreshToken.trim());
        Optional<RefreshTokenEntity> tokenEntityOpt = refreshTokenRepository.findByTokenHash(hashRefreshToken);

        if (tokenEntityOpt.isEmpty()) {
            return false;
        }

        RefreshTokenEntity tokenEntity = tokenEntityOpt.get();
        if (tokenEntity.isRevoked()) {
            return false;
        }

        tokenEntity.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(tokenEntity);
        return true;
    }
}
