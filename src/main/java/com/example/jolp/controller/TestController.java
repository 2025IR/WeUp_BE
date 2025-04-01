package com.example.jolp.controller;

import com.example.jolp.dto.LoginDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @PostMapping("/validation")
    public String testValidation(@Valid @RequestBody LoginDTO loginDTO) {
        return "검증 성공: " + loginDTO.getUsername();
    }
} 