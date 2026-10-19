package com.dynamicform.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Builder 
public class AuthResponseDto {

    private String token;

    private String refreshToken;

    private UserDto user;

    private LocalDateTime expiresAt;
}
