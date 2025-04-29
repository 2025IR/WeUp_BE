package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderDelegateRequestDTO {
    private Long projectId;
    private Long newLeaderId;
}