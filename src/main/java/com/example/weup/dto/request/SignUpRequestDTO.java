package com.example.weup.dto.request;

import lombok.Data;

@Data
public class SignUpRequestDTO {
    private String name;
    private String email;
    private String password;
} 