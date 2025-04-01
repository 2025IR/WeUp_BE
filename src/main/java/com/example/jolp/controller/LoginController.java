package com.example.jolp.controller;

import com.example.jolp.constant.Code;
import com.example.jolp.dto.CustomUserDetails;
import com.example.jolp.dto.LoginDTO;
import com.example.jolp.dto.TokenRequestDTO;
import com.example.jolp.dto.TokenResponseDTO;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.jwt.JWTUtil;
import com.example.jolp.repository.jwtUserRepository;
import com.example.jolp.service.AuthService;
import com.example.jolp.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class LoginController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        TokenResponseDTO tokens = authService.authenticateAndGenerateTokens(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );

        // 토큰을 헤더에 추가
        response.addHeader("Authorization", "Bearer " + tokens.getAccessToken());
        response.addHeader("Refresh-token", "Bearer " + tokens.getRefreshToken());

        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRequestDTO request, HttpServletResponse response) {
        TokenResponseDTO tokens = authService.refreshToken(request.getRefreshToken());

        // 토큰을 헤더에 추가
        response.addHeader("Authorization", "Bearer " + tokens.getAccessToken());
        response.addHeader("Refresh-token", "Bearer " + tokens.getRefreshToken());

        return ResponseEntity.ok(tokens);
    }
}
