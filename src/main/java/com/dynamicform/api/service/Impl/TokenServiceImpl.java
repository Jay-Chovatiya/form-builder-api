package com.dynamicform.api.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.dynamicform.api.config.JwtProperties;
import com.dynamicform.api.entity.UserEntity;
import com.dynamicform.api.service.TokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenServiceImpl implements TokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public TokenServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = getSignInKey(jwtProperties.getSecret());
    }

    @Override
    public Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    @Override
    public String generateJWTToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(jwtProperties.getExpirationMs())))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, UserEntity user) {
        try {
            Claims claims = extractAllClaims(token);

            String subject = claims.getSubject();

            return subject != null
                    && subject.equals(user.getId().toString());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    @Override
    public String hashRefreshToken(String refreshToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash).toUpperCase();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSignInKey(String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
