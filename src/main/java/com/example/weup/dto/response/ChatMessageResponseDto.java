package com.example.weup.dto.response;

import com.example.weup.entity.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ChatMessageResponseDto {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String message;

    private LocalDateTime sentAt;

    public static ChatMessageResponseDto fromEntity(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .messageId(chatMessage.getMessageId())
                .senderId(chatMessage.getUser().getUserId())
                .senderName(chatMessage.getUser().getName())
                .message(chatMessage.getMessage())
                .sentAt(chatMessage.getSentAt())
                .build();
    }
}
