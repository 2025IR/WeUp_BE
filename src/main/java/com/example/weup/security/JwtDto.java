package com.example.weup.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtDto {

    private String accessToken;

    private String refreshToken;

}
