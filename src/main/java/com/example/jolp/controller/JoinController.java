package com.example.jolp.controller;

import com.example.jolp.dto.JoinDTO;
import com.example.jolp.service.JoinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class JoinController {

    private final JoinService joinService;

    @PostMapping("/signup")
    public String signup(@Valid @RequestBody JoinDTO joinDTO) {
        joinService.joinProcess(joinDTO);

        String username = joinDTO.getUsername();
        System.out.println(username);

        return "회원가입하신 걸 환영합니다, " + username + "님!";
    }
}