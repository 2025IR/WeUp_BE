package com.example.weup.jwt;

import com.example.weup.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {

    private final SecretKey secretKey;
    private final UserRepository userRepository;

    @Autowired
    public JWTUtil(@Value("${jwt.secret}") String secret, UserRepository userRepository) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.userRepository = userRepository;
    }

    // 검증
    public Long getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // 서명 키 설정
                .build()
                .parseClaimsJws(token) // 토큰 검증 및 Claims 파싱
                .getBody()
                .get("userId", Long.class);
    }

    public String getType(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("type", String.class); // 토큰 타입 가져오기 (access 또는 refresh)
    }

    public String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
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

    // 수정 필요
    public String createAccessToken(Long userId, String role, long expiredMs) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "access") // 액세스 토큰 표시
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 수정 필요
    public String createRefreshToken(Long userId, long expiredMs) {
        return Jwts.builder()
                .claim("userId", userId)
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