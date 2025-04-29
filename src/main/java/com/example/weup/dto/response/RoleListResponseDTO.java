package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoleListResponseDTO {
    private Long roleId;
    private String roleName;
    private String roleColor;
}
