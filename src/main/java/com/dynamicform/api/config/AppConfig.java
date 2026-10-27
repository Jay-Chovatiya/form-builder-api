package com.dynamicform.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import com.dynamicform.api.config.securityConfiguration.SpaCsrfTokenRequestHandler;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder getBcryptEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfTokenRequestHandler xorCsrfTokenRequestHandler() {
        return new XorCsrfTokenRequestAttributeHandler();
    }

    @Bean
    public SpaCsrfTokenRequestHandler spaCsrfTokenRequestHandler(CsrfTokenRequestHandler xorCsrfTokenRequestHandler) {
        return new SpaCsrfTokenRequestHandler(xorCsrfTokenRequestHandler);
    }
}
