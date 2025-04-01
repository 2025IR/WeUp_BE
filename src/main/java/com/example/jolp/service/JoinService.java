package com.example.jolp.service;

import com.example.jolp.dto.JoinDTO;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.repository.jwtUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final jwtUserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void joinProcess(JoinDTO joinDTO) {
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();
        LocalDateTime PWExpirationDate = LocalDate.now().plusDays(90).atStartOfDay();

        System.out.println(username);
        System.out.println(password);

        // username 중복 확인
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(username);
        }

        if (username == null || password == null) {
            throw new AuthenticationServiceException(username);
        }

        // 새 사용자 저장
        UserEntity data = new UserEntity();
        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setRole("ROLE_ADMIN");
        data.setPasswordExpirationDate(PWExpirationDate);
        userRepository.save(data);
    }
}
