package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectInviteRequestDTO {
    private Long projectId;
    private String email;
}