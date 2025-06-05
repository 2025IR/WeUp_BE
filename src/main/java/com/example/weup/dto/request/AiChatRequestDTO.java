package com.example.weup.dto.request;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AiChatRequestDTO {

    private Long senderId;

    private String userInput;

    private Long projectId;
}
