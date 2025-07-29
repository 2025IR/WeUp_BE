package com.example.weup.dto.response;

import com.example.weup.entity.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatMessageResponseDto {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String message;

    private Boolean isImage;

    private LocalDateTime sentAt;
}
