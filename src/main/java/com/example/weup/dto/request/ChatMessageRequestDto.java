package com.example.weup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChatMessageRequestDto {

    private Long projectId;  // ????

    private String senderId;

    private String message;

    private LocalDateTime sentAt;
}
