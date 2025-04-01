package com.example.jolp.controller;

import com.example.jolp.constant.Code;
import com.example.jolp.dto.DataResponseDTO;
import com.example.jolp.dto.UserDTO;
import com.example.jolp.dto.UserIdRequestDTO;
import com.example.jolp.jwt.JWTUtil;
import com.example.jolp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JWTUtil jwtUtil;

    @PostMapping("/profile")
    public DataResponseDTO<UserDTO> profile(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException(Code.UNAUTHORIZED.getMessage("Authorization 헤더가 필요합니다"));
        }
        
        String accessToken = token.substring(7);
        jwtUtil.validateToken(accessToken);
        
        String username = jwtUtil.getUsername(accessToken);
        return DataResponseDTO.of(userService.findByUsername(username));
    }
}
