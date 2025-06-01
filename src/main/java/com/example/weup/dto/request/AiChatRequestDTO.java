package com.example.weup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequestDTO {

    private String massage;

    private Long projectId;
}
