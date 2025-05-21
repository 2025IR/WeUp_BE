package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequestDTO {
    private Long projectId;
    private String roleName;
    private String roleColor;

}
