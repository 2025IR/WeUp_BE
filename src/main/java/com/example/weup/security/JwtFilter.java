package com.example.weup.security;

import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.exception.JwtAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final List<String> WHITE_LIST = List.of(
            "/user/signIn", "/user/signup", "/user/reissuetoken", "/user/email", "/user/email/check",
            "/error", "/ws/", "/ai/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (WHITE_LIST.stream().anyMatch(request.getRequestURI()::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtil.resolveToken(request);

        if (token == null) {
            log.warn("Authorization 헤더 없음");
            jwtAuthenticationEntryPoint.commence(request, response, new BadCredentialsException("토큰 없음"));
            return;
        }

        if (jwtUtil.isExpired(token)) {
            log.warn("JWT 만료");
            jwtAuthenticationEntryPoint.commence(request, response, new BadCredentialsException("토큰 만료"));
            return;
        }

        Long userId = jwtUtil.getUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request,response);
    }
}