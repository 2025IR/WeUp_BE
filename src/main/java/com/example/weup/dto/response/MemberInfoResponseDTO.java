package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInfoResponseDTO {
    private Long memberId;
    private String name;
    private String email;
    private String profileImage;
    private String phoneNumber;
    private Boolean isLeader;
    private List<Long> roleIds;
}