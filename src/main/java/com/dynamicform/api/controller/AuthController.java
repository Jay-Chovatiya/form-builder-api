package com.dynamicform.api.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicform.api.common.ratelimit.RateLimit;
import com.dynamicform.api.common.response.ApiResponse;
import com.dynamicform.api.config.JwtProperties;
import com.dynamicform.api.dto.AuthResponseDto;
import com.dynamicform.api.dto.LoginDto;
import com.dynamicform.api.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final String REFRESH_COOKIE_NAME = "refreshToken";

    @PostMapping("/login")
    @RateLimit("login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpServletResponse) {

        AuthResponseDto response = authService.login(request)
                .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password, or account is disabled."));

        if (response.getRefreshToken() != null && !response.getRefreshToken().isBlank()) {
            setRefreshTokenCookie(httpServletResponse, response.getRefreshToken(), request.isRememberMe());
        }

        setXsrfCookie(httpRequest, httpServletResponse);

        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpServletResponse) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Refresh token is missing."));
        }

        AuthResponseDto response = authService.refreshToken(refreshToken)
                .orElse(null);

        if (response == null) {
            deleteRefreshTokenCookie(httpServletResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Invalid or expired refresh token."));
        }

        if (response.getRefreshToken() != null && !response.getRefreshToken().isBlank()) {
            setRefreshTokenCookie(httpServletResponse, response.getRefreshToken(), true);
        }

        setXsrfCookie(httpRequest, httpServletResponse);

        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
    }

    @PostMapping("/revoke-token")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpServletResponse) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.revokeToken(refreshToken.trim());
        }

        deleteRefreshTokenCookie(httpServletResponse);
        csrfTokenRepository.saveToken(null, httpRequest, httpServletResponse);
        return ResponseEntity.ok(ApiResponse.ok(null, "Token revoked successfully"));
    }

    private void setXsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
        }
        csrfTokenRepository.saveToken(csrfToken, request, response);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, boolean rememberMe) {
        long maxAgeMs = rememberMe
                ? jwtProperties.getRefreshExpirationMs() * 7
                : jwtProperties.getRefreshExpirationMs();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
    }
}
