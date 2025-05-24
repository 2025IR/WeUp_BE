package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteRoleRequestDTO {
    private Long projectId;
    private Long roleId;
}
