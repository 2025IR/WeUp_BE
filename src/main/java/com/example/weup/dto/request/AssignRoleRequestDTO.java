package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignRoleRequestDTO {
    private Long projectId;
    private Long memberId;
    private List<Long> roleIds;
}
