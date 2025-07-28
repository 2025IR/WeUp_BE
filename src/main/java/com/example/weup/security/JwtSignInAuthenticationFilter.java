package com.example.weup.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
public class JwtSignInAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public JwtSignInAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper,
                                         AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler) {
        setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("/user/signIn");
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        this.objectMapper = objectMapper;
    }

//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
//
//        try {
//            SignInRequestDto requestDto = objectMapper.readValue(request.getInputStream(), SignInRequestDto.class);
//            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword());
//
//            return getAuthenticationManager().authenticate(authenticationToken);
//        }
//        catch (IOException e) {
//            throw new AuthenticationServiceException("", e);
//        }
//    }
}
