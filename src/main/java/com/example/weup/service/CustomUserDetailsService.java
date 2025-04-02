package com.example.weup.service;

import com.example.weup.dto.security.CustomUserDetails;
import com.example.weup.entity.UserEntity;
import com.example.weup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("findByEmail() 호출: " + email);

        UserEntity userData = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("사용자 찾을 수 없음: " + email);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
                });

        System.out.println("찾은 사용자: " + userData);
        System.out.println(new CustomUserDetails(userData));
        return new CustomUserDetails(userData);
    }


}