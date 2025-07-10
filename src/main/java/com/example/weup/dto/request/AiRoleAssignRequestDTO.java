package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AiRoleAssignRequestDTO {

    private Long projectId;

    private String userName;

    private String roleName;
}
