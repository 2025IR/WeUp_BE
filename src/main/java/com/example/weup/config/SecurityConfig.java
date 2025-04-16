package com.example.weup.config;

import com.example.weup.security.*;
import com.example.weup.security.exception.JwtAccessDeniedHandler;
import com.example.weup.security.exception.JwtAuthenticationEntryPoint;
import com.example.weup.security.exception.JwtAuthenticationFailureHandler;
import com.example.weup.security.exception.JwtAuthenticationSuccessHandler;
import com.example.weup.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

     private final CustomUserDetailsService customUserDetailsService;

     private final JwtFilter jwtFilter;

     private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;

     private final JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler;

     private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

     private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

     private final ObjectMapper objectMapper;

     @Bean
     public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

         http
                 .csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                 .exceptionHandling(exception -> exception
                         .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                         .accessDeniedHandler(jwtAccessDeniedHandler)
                 )

                 .authorizeHttpRequests(auth -> auth
                         .requestMatchers("/user/signIn", "/user/signup", "/user/reissuetoken").permitAll()
                         .requestMatchers("/project/**").hasRole("USER")
                         .anyRequest().authenticated()
                 )

                 .addFilterAt(jwtSignInAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

         return http.build();
     }

     @Bean
     public JwtSignInAuthenticationFilter jwtSignInAuthenticationFilter(AuthenticationManager authenticationManager) {
         return new JwtSignInAuthenticationFilter(authenticationManager, objectMapper, jwtAuthenticationSuccessHandler, jwtAuthenticationFailureHandler);
     }

     @Bean
     public AuthenticationManager authenticationManager() {
         DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
         provider.setUserDetailsService(customUserDetailsService);
         provider.setPasswordEncoder(passwordEncoder());

         return new ProviderManager(provider);
     }

     @Bean
     public PasswordEncoder passwordEncoder() {
         return new BCryptPasswordEncoder();
     }

}
