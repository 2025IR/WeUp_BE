package com.example.weup.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "🎉 팀 프로젝트 관리 웹, we:up에 오신 걸 환영합니다!";
    }
}