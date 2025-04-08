package com.example.weup.security;

import com.example.weup.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

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

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .setSubject("RefreshToken")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("userId", userId)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        return null;
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

//public class JwtUtil {
//
//    private final SecretKey secretKey;
//    private final UserRepository userRepository;
//
//    @Autowired
//    public JwtUtil(@Value("${jwt.secret}") String secret, UserRepository userRepository) {
//        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//        this.userRepository = userRepository;
//    }
//
//    // 검증
//    public Long getUserId(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(secretKey) // 서명 키 설정
//                .build()
//                .parseClaimsJws(token) // 토큰 검증 및 Claims 파싱
//                .getBody()
//                .get("userId", Long.class);
//    }
//
//    public String getType(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(secretKey)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .get("type", String.class); // 토큰 타입 가져오기 (access 또는 refresh)
//    }
//
//    public String getRole(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(secretKey)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .get("role", String.class);
//    }
//
//    public Boolean isExpired(String token) {
//        Date expiration = Jwts.parserBuilder()
//                .setSigningKey(secretKey)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getExpiration(); // 만료 날짜 가져오기
//        return expiration.before(new Date()); // 현재 시간과 비교
//    }
//
//    public String createAccessToken(Long userId, String role) {
//        return Jwts.builder()
//                .claim("userId", userId)
//                .claim("role", role)
//                .claim("type", "access") // 액세스 토큰 표시
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis()))
//                .signWith(secretKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public String createRefreshToken(Long userId) {
//        return Jwts.builder()
//                .claim("userId", userId)
//                .claim("type", "refresh") // 리프레시 토큰 표시
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis()))
//                .signWith(secretKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public void validateToken(String token) {
//        try {
//            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
//        } catch (ExpiredJwtException e) {
//            throw new RuntimeException("Token expired");
//        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e) {
//            throw new RuntimeException("Invalid token");
//        }
//    }
//}