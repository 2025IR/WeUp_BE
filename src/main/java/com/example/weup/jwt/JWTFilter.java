package com.example.weup.jwt;

import com.example.weup.dto.security.CustomUserDetails;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")) {
            log.debug("token null");
            filterChain.doFilter(request,response);
            return;
        }

        log.debug("authorization now");
        String token = authorization.split(" ")[1];

        if(jwtUtil.isExpired(token)) {
            log.debug("token expired");
            filterChain.doFilter(request,response);
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);

        // 사용자 ID로 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElse(new User()); // 사용자를 찾지 못하면 빈 객체 사용
        
        if (user.getId() == null) {
            // 사용자를 찾을 수 없는 경우 로그 기록 후 계속 진행
            log.warn("User not found with ID: {}", userId);
            user.setRole(role);
        }

        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request,response);
    }
}