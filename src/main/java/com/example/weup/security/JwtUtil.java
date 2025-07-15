package com.example.weup.security;

import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private final StringRedisTemplate redisTemplate;

    private final SecretKey secretKey;

    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .setSubject("AccessToken")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("userId", userId)
                .claim("role", role)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Transactional
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        String refreshToken = Jwts.builder()
                .setSubject("RefreshToken")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("userId", userId)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        redisTemplate.opsForValue().set("refreshToken:" + userId, refreshToken, jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return refreshToken;
    }

    public String resolveToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    public boolean isExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        }
        catch (Exception e) {
            log.warn("JWT 만료 여부 확인 중 예외 발생 : {}", e.getMessage());
            return true;
        }
    }

    public Long getUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
