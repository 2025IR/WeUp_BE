package com.example.weup.security.exception;

import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.entity.User;
import com.example.weup.security.JwtCookieFactory;
import com.example.weup.security.JwtDto;
import com.example.weup.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();

        String accessToken = jwtUtil.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId());

        user.renewalToken(refreshToken);

        ResponseCookie refreshCookie = JwtCookieFactory.createRefreshCookie(refreshToken);

        JwtDto jwtDto = JwtDto.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .build();

        DataResponseDTO<JwtDto> dataResponse = DataResponseDTO.of(jwtDto, "로그인 성공");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.getWriter().write(objectMapper.writeValueAsString(dataResponse));

        log.info("로그인 성공 - USER_ID : {}", user.getUserId());
    }
}
