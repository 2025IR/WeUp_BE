package com.example.jolp.service;

import com.example.jolp.GeneralException;
import com.example.jolp.constant.Code;
import com.example.jolp.dto.UserDTO;
import com.example.jolp.entity.UserEntity;
import com.example.jolp.repository.jwtUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final jwtUserRepository userRepository;

    public UserDTO profile(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new GeneralException(Code.NOT_FOUND));
        return new UserDTO(user);
    }
    
    public UserDTO findByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new GeneralException(Code.USER_NOT_FOUND));
        return new UserDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }
}
