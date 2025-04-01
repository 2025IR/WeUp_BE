package com.example.jolp.service;

import com.example.jolp.dto.CustomUserDetails;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.repository.jwtUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final jwtUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("findByUsername() 호출: " + username);

        UserEntity userData = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("사용자 찾을 수 없음: " + username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        System.out.println("찾은 사용자: " + userData);
        System.out.println(new CustomUserDetails(userData));
        return new CustomUserDetails(userData);
    }


}