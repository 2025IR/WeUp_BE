package com.example.jolp.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWTUtil : 0.12.3
@Component
@Slf4j
public class JWTUtil {

    private final SecretKey secretKey;

    public JWTUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 검증
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // 서명 키 설정
                .build()
                .parseClaimsJws(token) // 토큰 검증 및 Claims 파싱
                .getBody()
                .get("username", String.class); // username 가져오기
    }


    public String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public String gettype(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("type", String.class);
    }


    public Boolean isExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration(); // 만료 날짜 가져오기
        return expiration.before(new Date()); // 현재 시간과 비교
    }




    // 생성
    public String createJwt(String username, String role, long expiredMs) {
        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs)) // 만료 시간
                .signWith(secretKey, SignatureAlgorithm.HS256) // 서명 알고리즘 설정
                .compact();
    }

    public String createAccessToken(String username, String role, long expiredMs) {
        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access") // 액세스 토큰 표시
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String username, long expiredMs) {
        return Jwts.builder()
                .claim("username", username)
                .claim("type", "refresh") // 리프레시 토큰 표시
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expired");
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid token");
        }
    }

}