package com.example.weup.dto.request;

import lombok.Getter;

@Getter
public class PasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
}
