package com.example.jolp.service;

import com.example.jolp.config.JwtProperties;
import com.example.jolp.constant.Code;
import com.example.jolp.dto.CustomUserDetails;
import com.example.jolp.dto.TokenResponseDTO;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.jwt.JWTUtil;
import com.example.jolp.repository.jwtUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final jwtUserRepository userRepository;
    private final JwtProperties jwtProperties;

    /**
     * 사용자 인증 및 JWT 토큰 생성
     */
    public TokenResponseDTO authenticateAndGenerateTokens(String username, String password) {
        try {
            validateCredentials(username, password);

            // 1. 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 2. JWT 생성
            String accessToken = jwtUtil.createAccessToken(
                    username,
                    userDetailsService.loadUserByUsername(username).getAuthorities().toString(),
                    jwtProperties.getAccessToken().getExpiration()
            );

            String refreshToken = jwtUtil.createRefreshToken(
                    username,
                    jwtProperties.getRefreshToken().getExpiration()
            );

            // 3. DB에 리프레시 토큰 저장
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));

            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            return new TokenResponseDTO(accessToken, refreshToken);

        } catch (AuthenticationException e) {
            handleAuthenticationException(e);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(Code.INTERNAL_ERROR.getMessage(e));
        }
    }

    /**
     * 리프레시 토큰을 사용한 액세스 토큰 재발급
     */
    @Transactional
    public TokenResponseDTO refreshToken(String refreshToken) {
        try {
            // 1. 리프레시 토큰 검증
            jwtUtil.validateToken(refreshToken);

            // 2. 리프레시 토큰으로 사용자 조회
            UserEntity user = userRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException(Code.UNAUTHORIZED.getMessage("Refresh token not found")));

            // 3. 새로운 토큰 발급
            String newAccessToken = jwtUtil.createAccessToken(
                    user.getUsername(),
                    userDetailsService.loadUserByUsername(user.getUsername()).getAuthorities().toString(),
                    jwtProperties.getAccessToken().getExpiration()
            );

            String newRefreshToken = jwtUtil.createRefreshToken(
                    user.getUsername(),
                    jwtProperties.getRefreshToken().getExpiration()
            );

            // 4. 새로운 리프레시 토큰을 DB에 저장
            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);

            return new TokenResponseDTO(newAccessToken, newRefreshToken);
        } catch (Exception e) {
            throw new RuntimeException(Code.INTERNAL_ERROR.getMessage(e));
        }
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new BadCredentialsException(Code.BAD_REQUEST.getMessage("Username is required"));
        }
        if (password == null || password.isBlank()) {
            throw new BadCredentialsException(Code.BAD_REQUEST.getMessage("Password is required"));
        }
    }

    private void handleAuthenticationException(AuthenticationException e) {
        if (e instanceof BadCredentialsException) {
            throw new BadCredentialsException(Code.BAD_CREDENTIALS.getMessage());
        } else if (e instanceof UsernameNotFoundException) {
            throw new UsernameNotFoundException(Code.USER_NOT_FOUND.getMessage());
        } else {
            throw new RuntimeException(Code.UNAUTHORIZED.getMessage(e));
        }
    }
}
