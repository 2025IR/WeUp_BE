package com.example.weup.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignUpRequestDto {
    private String name;
    private String email;
    private String password;
} 