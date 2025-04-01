package com.example.jolp.service;

import com.example.jolp.config.JwtProperties;
import com.example.jolp.dto.CustomUserDetails;
import com.example.jolp.dto.TokenResponseDTO;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.jwt.JWTUtil;
import com.example.jolp.repository.jwtUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private jwtUserRepository userRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private UserEntity testUser;
    private CustomUserDetails testUserDetails;
    private Authentication testAuthentication;
    private String testUsername;
    private String testPassword;
    private String testAccessToken;
    private String testRefreshToken;

    @BeforeEach
    void setUp() {
        testUsername = "testUser";
        testPassword = "testPassword";
        testAccessToken = "testAccessToken";
        testRefreshToken = "testRefreshToken";

        testUser = new UserEntity();
        testUser.setUsername(testUsername);
        testUser.setPassword(testPassword);
        testUser.setRole("ROLE_USER");

        testUserDetails = new CustomUserDetails(testUser);
        testAuthentication = new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());

        // JWT Properties 설정
        JwtProperties.AccessToken accessToken = new JwtProperties.AccessToken();
        accessToken.setExpiration(1800000L); // 30분
        JwtProperties.RefreshToken refreshToken = new JwtProperties.RefreshToken();
        refreshToken.setExpiration(604800000L); // 7일
        when(jwtProperties.getAccessToken()).thenReturn(accessToken);
        when(jwtProperties.getRefreshToken()).thenReturn(refreshToken);
    }

    @Test
    void authenticateAndGenerateTokens_Success() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(testAuthentication);
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(jwtUtil.createAccessToken(any(), any(), any())).thenReturn(testAccessToken);
        when(jwtUtil.createRefreshToken(any(), any())).thenReturn(testRefreshToken);

        // When
        TokenResponseDTO result = authService.authenticateAndGenerateTokens(testUsername, testPassword);

        // Then
        assertNotNull(result);
        assertEquals(testAccessToken, result.getAccessToken());
        assertEquals(testRefreshToken, result.getRefreshToken());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void refreshToken_Success() {
        // Given
        when(userRepository.findByRefreshToken(testRefreshToken)).thenReturn(Optional.of(testUser));
        when(jwtUtil.createAccessToken(any(), any(), any())).thenReturn(testAccessToken);
        when(jwtUtil.createRefreshToken(any(), any())).thenReturn(testRefreshToken);

        // When
        TokenResponseDTO result = authService.refreshToken(testRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(testAccessToken, result.getAccessToken());
        assertEquals(testRefreshToken, result.getRefreshToken());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void authenticateAndGenerateTokens_InvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> 
            authService.authenticateAndGenerateTokens(testUsername, testPassword)
        );
    }

    @Test
    void refreshToken_InvalidToken() {
        // Given
        when(userRepository.findByRefreshToken(testRefreshToken)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            authService.refreshToken(testRefreshToken)
        );
    }
} 