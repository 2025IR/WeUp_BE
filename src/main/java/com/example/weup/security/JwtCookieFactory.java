package com.example.weup.security;

import org.springframework.http.ResponseCookie;

public class JwtCookieFactory {

    private static final String COOKIE_NAME = "refresh_token";
    private static final long MAX_AGE = 7 * 24 * 60 * 60;

    public static ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // todo. 배포 시 true로
                .path("/")
                .sameSite("Strict") //todo. lax, None 확인해서 CORS 프론트 보고 수정하기
                .maxAge(MAX_AGE)
                .build();
    }
}

