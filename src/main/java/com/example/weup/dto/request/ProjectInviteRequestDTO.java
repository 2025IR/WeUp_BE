package com.example.weup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectInviteRequestDTO {
    private Long projectId;
    @NotBlank(message = "초대할 이메일을 입력해주세요.")
    private String email;
}