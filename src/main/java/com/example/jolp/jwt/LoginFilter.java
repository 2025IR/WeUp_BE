package com.example.jolp.jwt;

import com.example.jolp.dto.CustomUserDetails;
import com.example.jolp.dto.LoginDTO;
import com.example.jolp.constant.Code;
import com.example.jolp.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

@RequiredArgsConstructor
@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;



    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        ObjectMapper objectMapper = new ObjectMapper();
        LoginDTO loginDTO;

        try {
            loginDTO = objectMapper.readValue(request.getInputStream(), LoginDTO.class);
        } catch (IOException e) {
            throw new AuthenticationServiceException("요청 본문을 읽는 중 오류가 발생했습니다.");
        }

        validateLoginDTO(loginDTO);

        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);
        return authenticationManager.authenticate(authToken);
    }

    // DTO 검증 로직 추가
    private void validateLoginDTO(LoginDTO loginDTO) {
        if (loginDTO.getUsername() == null || loginDTO.getUsername().isBlank()) {
            throw new AuthenticationServiceException("Username은 필수 값입니다.");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
            throw new AuthenticationServiceException("Password는 필수 값입니다.");
        }
    }


    // Exception handler for AuthenticationServiceException
    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationServiceException(AuthenticationServiceException e) {
        // Respond with a standardized error response
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseDTO.of(Code.BAD_REQUEST, e.getMessage()));
    }



    @Override //JWT 발급
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal(); // 인증된 사용자 정보
        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String accesstoken = jwtUtil.createAccessToken(username, role, 60 * 60 * 10000L);
        response.addHeader("Authorization", "Bearer " + accesstoken);

        String refreshtoken = jwtUtil.createRefreshToken(username, 60 * 60 * 10000L);
        response.addHeader("Refresh-token", "Bearer " + refreshtoken);


        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(new HashMap<>() {{
            put("accesstoken", "Bearer " + accesstoken);
        }}));
        response.getWriter().write(new ObjectMapper().writeValueAsString(new HashMap<>() {{
            put("refreshtoken", "Bearer " + refreshtoken);
        }}));
        //그냥 보기쉽게 본문에 추가
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        // 1. HTTP 상태 코드 설정 (401: Unauthorized)
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 2. 에러 메시지 및 코드 매핑
        String errorMessage;
        Code errorCode;

        if (failed instanceof UsernameNotFoundException) {
            errorMessage = "사용자를 찾을 수 없습니다.";
            errorCode = Code.USER_NOT_FOUND;
        } else if (failed instanceof BadCredentialsException) {
            errorMessage = "아이디 또는 비밀번호가 잘못되었습니다.";
            errorCode = Code.UNAUTHORIZED;
        } else if (failed instanceof DisabledException) {
            errorMessage = "계정이 비활성화되었습니다.";
            errorCode = Code.UNAUTHORIZED;
        } else if (failed instanceof LockedException) {
            errorMessage = "계정이 잠겨 있습니다.";
            errorCode = Code.UNAUTHORIZED;
        } else if (failed instanceof AccountStatusException) {
            errorMessage = "비밀번호가 만료되었습니다.";
            errorCode = Code.USER_PASSWORD_EXPIRED;
        } else {
            errorMessage = "인증에 실패하였습니다.";
            errorCode = Code.INTERNAL_ERROR;
        }

        // 3. ErrorResponseDTO 사용
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of(errorCode, errorMessage);

        // 4. 응답 작성
        response.setStatus(errorCode.getHttpStatus().value()); // HttpStatus 설정
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}