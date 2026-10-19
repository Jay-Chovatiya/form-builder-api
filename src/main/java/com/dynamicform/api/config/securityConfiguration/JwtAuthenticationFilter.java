package com.dynamicform.api.config.securityConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dynamicform.api.repository.UserRepository;
import com.dynamicform.api.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component 
@RequiredArgsConstructor  
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final Long userId = tokenService.extractUserId(jwt);

            if (userId != null && userId > 0 && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.isActive() && tokenService.isTokenValid(jwt, user)) {
                        List<SimpleGrantedAuthority> authorities = user.getRole() != null
                                ? List.of(
                                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name()),
                                        new SimpleGrantedAuthority(user.getRole().name())
                                  )
                                : Collections.emptyList();

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else if (!user.isActive()) {
                        log.warn("Authentication skipped: user with ID {} is deactivated", userId);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            log.debug("JWT authentication error trace: ", e);
        }

        filterChain.doFilter(request, response);
    }
}
