package com.example.weup.config;

import com.example.weup.jwt.JWTFilter;
import com.example.weup.jwt.JWTUtil;
import com.example.weup.jwt.JwtAccessDeniedHandler;
import com.example.weup.jwt.JwtAuthenticationEntryPoint;
import com.example.weup.jwt.JwtAuthenticationFilter;
import com.example.weup.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

 /**
 Spring Security를 통해 JWT 기반의 인증/인가를 설정
 HTTP 요청마다 JWT를 검증하고, 특정 경로에 대해 접근 권한을 관리하며,
 Stateless 방식으로 세션 없는 인증을 처리하는 구성을 정의
 (클라이언트의 인증 상태를 서버에 저장하는 게 아닌, 요청마다 JWT로 인증하기 때문)
 */

@Configuration
@EnableWebSecurity // 시큐리티를 위한 설정 클래스
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    // Spring Security의 인증 관련 설정 가져오고, AuthenticationManager 생성
    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    // 인증 요청을 처리하는 주요 컴포넌트, 로그인 시 사용자 인증에 사용
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
    // 비밀번호를 암호화하거나 저장된 비밀번호와 입력된 비밀번호를 비교할 때 사용
    // UserDetailsService에서 사용자 비밀번호를 확인할 때 사용
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("http://localhost:8080"); // 허용할 도메인
        configuration.addAllowedOriginPattern("ws://localhost:8080"); // Pattern 붙이면 포트 와카 허용
        configuration.addAllowedMethod("*"); // HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용
        return source;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER"); // ADMIN은 USER 권한도 자동 포함
        return roleHierarchy;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //csrf disable -> JWT는 Stateless 방식으로 인증을 처리하기 때문에 불필요
        http.csrf((auth) -> auth.disable());

        // CORS 활성화
        http.cors((cors) -> cors.configurationSource(corsConfigurationSource()));

        //Form 로그인 방식 disable -> JSON으로 된 로그인 정보 처리
        http.formLogin((auth) -> auth.disable());

        //http basic 인증 장식 disable -> JWT 기반 인증 방식 사용
        http.httpBasic((auth) -> auth.disable());

        http.exceptionHandling((exceptionHandling) -> exceptionHandling
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler));

        //경로별 인가 작업
        http.authorizeHttpRequests((auth) ->
                auth.requestMatchers("/user/login", "/user/signup", "/user/token", "/", "/chat/**").permitAll() // 접근을 전체 허용할 경로 설정
                        .requestMatchers("/admin").hasRole("ADMIN") // 권한에 따른 접근 허용 설정
                        .anyRequest().authenticated()); // 그 외에는 인증을 요구

        // JWT 인증 필터
        http.addFilterBefore(new JWTFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);
        
        // 로그인 처리 필터
        http.addFilterBefore(new JwtAuthenticationFilter(authenticationManager(authenticationConfiguration), jwtUtil, objectMapper, userRepository, jwtProperties), JWTFilter.class);

        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        //세션 관리를 stateless로, JWT를 사용하니까 서버에 세션 상태를 저장하지 않음

        return http.build();
        //구성된 httpSecurity 객체를 반환하고, 반환된 객체로 스프링 스큐리티가 필터 체인을 생성함
    }
}