package com.dynamicform.api.config.securityConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dynamicform.api.entity.Role;
import com.dynamicform.api.entity.UserEntity;
import com.dynamicform.api.repository.UserRepository;
import com.dynamicform.api.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withoutAuthHeader_continuesChainWithoutAuth() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenService, userRepository);
    }

    @Test
    void doFilterInternal_withNonBearerHeader_continuesChainWithoutAuth() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenService, userRepository);
    }

    @Test
    void doFilterInternal_withBlankBearerHeader_continuesChainWithoutAuth() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer   ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenService, userRepository);
    }

    @Test
    void doFilterInternal_validTokenAndActiveUser_setsAuthenticatedContext() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(tokenService.extractUserId(token)).thenReturn(10L);

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setUsername("john_doe");
        user.setRole(Role.Administrator);
        user.setActive(true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(tokenService.isTokenValid(token, user)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(user, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_Administrator")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("Administrator")));
    }

    @Test
    void doFilterInternal_userInactive_doesNotSetAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(tokenService.extractUserId(token)).thenReturn(20L);

        UserEntity user = new UserEntity();
        user.setId(20L);
        user.setActive(false);

        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).isTokenValid(any(), any());
    }

    @Test
    void doFilterInternal_userNotFound_doesNotSetAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(tokenService.extractUserId(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_tokenServiceThrowsException_catchesAndContinuesChain() throws ServletException, IOException {
        String token = "corrupted.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(tokenService.extractUserId(token)).thenThrow(new RuntimeException("Signature verification failed"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
