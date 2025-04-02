package com.example.weup.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinDTO {
    @NotBlank(message = "이름은 필수 작성 사항입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수 작성 사항입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 작성 사항입니다.")
    private String password;
} 