package com.example.weup.dto.response;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String message;

    public TokenResponseDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
} 