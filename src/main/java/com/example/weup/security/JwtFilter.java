package com.example.weup.security;

import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = jwtUtil.resolveToken(request);

        if(token == null) {
            log.debug("JWT 토큰 없음 또는 형식 오류");
            filterChain.doFilter(request,response);
            return;
        }

//        log.debug("authorization now");
//        String token = authorization.split(" ")[1];

        if(jwtUtil.isExpired(token)) {
            log.warn("JWT 토큰 만료");
            filterChain.doFilter(request,response);
            return;
        }

        Long userId = jwtUtil.getUserId(token);
//        String role = jwtUtil.getRole(token);

        // 사용자 ID로 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

//        if (user.getUserId() == null) {
//            // 사용자를 찾을 수 없는 경우 로그 기록 후 계속 진행
//            log.warn("User not found with ID: {}", userId);
//            user.setRole(role);
//        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request,response);
    }
}