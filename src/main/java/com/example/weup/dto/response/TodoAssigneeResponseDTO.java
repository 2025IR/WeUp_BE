package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TodoAssigneeResponseDTO {
    private Long memberId;
    private String name;
    private String profileImage;
}
