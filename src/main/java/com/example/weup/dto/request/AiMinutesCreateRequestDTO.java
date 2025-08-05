package com.example.weup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class AiMinutesCreateRequestDTO {
    private Long projectId;
    private String title;
    private String contents;
}
