package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditRoleRequestDTO {
    private Long projectId;
    private Long memberId;
    private String roleName;
    private Byte roleColor;
}
