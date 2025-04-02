package com.example.weup.jwt;

import com.example.weup.config.JwtProperties;
import com.example.weup.dto.security.CustomUserDetails;
import com.example.weup.dto.request.LoginDTO;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.ErrorResponseDTO;
import com.example.weup.entity.UserEntity;
import com.example.weup.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, 
                                 ObjectMapper objectMapper, UserRepository userRepository, JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
        // 로그인 URL 설정
        setFilterProcessesUrl("/user/login");
        // 필터가 처리할 HTTP 메서드 설정
        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/user/login", "POST"));
        // TODO 이 부분 자세히 찾아보기
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        LoginDTO loginDTO;
        try {
            loginDTO = objectMapper.readValue(request.getInputStream(), LoginDTO.class); // 요청 본문(json)을 DTO로 가져오기
        } catch (IOException e) {
            throw new AuthenticationServiceException("요청 본문을 읽는 중 오류가 발생했습니다.");
            // TODO 에러 처리 로직 세분화 - AuthenticationServiceException
            // TODO 메시지 중복 표기하는 방법?
        }

        validateLoginDTO(loginDTO);

        String email = loginDTO.getEmail();
        String password = loginDTO.getPassword();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);
        return authenticationManager.authenticate(authToken);
        // TODO authenticationManager에게 인증 과정 위임?
        // TODO 권한이 표기되지 않은 토큰을 받아 검증 후 권한이 포함된 토큰을 반환한다인듯?
    }

    private void validateLoginDTO(LoginDTO loginDTO) {
        if (loginDTO.getEmail() == null || loginDTO.getEmail().isBlank()) {
            throw new AuthenticationServiceException("이메일은 필수 값입니다.");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
            throw new AuthenticationServiceException("비밀번호는 필수 값입니다.");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        String email = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        // DB에서 사용자 검색
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        Long userId = user.getId();

        // 설정 파일에서 만료 시간 가져오기
        long accessTokenExpiration = jwtProperties.getAccessToken().getExpiration();
        long refreshTokenExpiration = jwtProperties.getRefreshToken().getExpiration();

        // 토큰 생성 (userId 사용)
        String accessToken = jwtUtil.createAccessToken(userId, role, accessTokenExpiration);
        String refreshToken = jwtUtil.createRefreshToken(userId, refreshTokenExpiration);

        // DB에 리프레시 토큰 저장
        user.setRefreshToken("Bearer " + refreshToken);
        userRepository.save(user);

        // 헤더에 토큰 추가
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("Refresh-Token", "Bearer " + refreshToken);

        // 응답 본문에 토큰 추가
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        HashMap<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", "Bearer " + accessToken);
        responseBody.put("refreshToken", "Bearer " + refreshToken);

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String errorMessage;
        ErrorInfo errorInfo;

        if (failed instanceof UsernameNotFoundException) {
            errorMessage = "사용자를 찾을 수 없습니다.";
            errorInfo = ErrorInfo.USER_NOT_FOUND;
        } else if (failed instanceof BadCredentialsException) {
            errorMessage = "아이디 또는 비밀번호가 잘못되었습니다.";
            errorInfo = ErrorInfo.UNAUTHORIZED;
        } else if (failed instanceof DisabledException) {
            errorMessage = "계정이 비활성화되었습니다.";
            errorInfo = ErrorInfo.UNAUTHORIZED;
        } else if (failed instanceof LockedException) {
            errorMessage = "계정이 잠겨 있습니다.";
            errorInfo = ErrorInfo.UNAUTHORIZED;
        } else if (failed instanceof AccountStatusException) {
            errorMessage = "비밀번호가 만료되었습니다.";
            errorInfo = ErrorInfo.USER_PASSWORD_EXPIRED;
        } else if (failed instanceof AuthenticationServiceException) {
            errorMessage = "로그인에 실패하였습니다.";
            errorInfo = ErrorInfo.BAD_REQUEST;
        } else {
            errorMessage = "인증에 실패하였습니다.";
            errorInfo = ErrorInfo.INTERNAL_ERROR;
        }


        ErrorResponseDTO errorResponse = ErrorResponseDTO.of(errorInfo, errorMessage);
        response.setStatus(errorInfo.getHttpStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
} 