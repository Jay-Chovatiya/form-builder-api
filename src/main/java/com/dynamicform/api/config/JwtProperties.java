package com.dynamicform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@ConfigurationProperties (prefix = "formstudio.jwt")
public class JwtProperties {
    public String secret;
    public long expirationMs;
    public long refreshExpirationMs;
}
