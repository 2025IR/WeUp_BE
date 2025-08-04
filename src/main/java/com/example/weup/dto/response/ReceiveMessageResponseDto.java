package com.example.weup.dto.response;

import com.example.weup.constant.DisplayType;
import com.example.weup.constant.SenderType;
import com.example.weup.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ReceiveMessageResponseDto {

    private Long senderId;

    private String senderName;

    private String senderProfileImage;

    private String message;

    private LocalDateTime sentAt;

    @JsonProperty("isImage")
    private boolean isImage;

    private SenderType senderType;

    private DisplayType displayType;

    public static ReceiveMessageResponseDto fromEntity(ChatMessage chatMessage) {
        return ReceiveMessageResponseDto.builder()
                .senderId(chatMessage.getMessageId())
                .senderName(chatMessage.getMember().getUser().getName())
                .message(chatMessage.getMessage())
                .sentAt(chatMessage.getSentAt())
                .isImage(chatMessage.getIsImage())
                .senderType(chatMessage.getSenderType())
                .displayType(chatMessage.getDisplayType())
                .build();
    }
}
