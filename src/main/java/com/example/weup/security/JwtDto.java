package com.example.weup.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtDto {
    private String accessToken;
    private String refreshToken;
    private Long userId;

    public JwtDto withoutRefreshToken() {
        return JwtDto.builder()
                .accessToken(this.accessToken)
                .userId(this.userId)
                .build();
    }
}
