package com.example.jolp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinDTO {
    @NotBlank(message = "이름은 필수 작성 사항입니다.")
    private String username;
    @NotBlank(message = "비밀번호는 필수 작성 사항입니다.")
    private String password;
}
