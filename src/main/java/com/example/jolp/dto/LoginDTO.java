package com.example.jolp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "이름은 2자 이상 50자 이하여야 합니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String username;
    @NotBlank(message = "비밀번호는 필수 작성 사항입니다.")
    private String password;
}