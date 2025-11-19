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
public class ReceiveMessageResponseDTO {

    private String uuid;

    private Long senderId;

    private String senderName;

    private String senderProfileImage;

    private String message;

    private LocalDateTime sentAt;

    @JsonProperty("isImage")
    private boolean isImage;

    @JsonProperty("isPrompt")
    private boolean isPrompt;

    private SenderType senderType;

    private DisplayType displayType;

    private String originalSenderName;

    private String originalMessage;

    private int unreadCount;

    public static ReceiveMessageResponseDTO fromRedisMessageDTO(RedisMessageDTO chatMessage) {
        return ReceiveMessageResponseDTO.builder()
                .uuid(chatMessage.getUuid())
                .senderId(chatMessage.getSenderType()==SenderType.MEMBER ? chatMessage.getMemberId() : null)
                .message(chatMessage.getMessage())
                .sentAt(chatMessage.getSentAt())
                .isImage(chatMessage.getIsImage())
                .isPrompt(chatMessage.getIsPrompt())
                .senderType(chatMessage.getSenderType())
                .displayType(chatMessage.getDisplayType())
                .originalMessage(chatMessage.getOriginalMessage())
                .originalSenderName(chatMessage.getOriginalSenderName())
                .build();
    }

    public static ReceiveMessageResponseDTO fromChatMessageEntity(ChatMessage chatMessage) {
        return ReceiveMessageResponseDTO.builder()
                .uuid(chatMessage.getUuid())
                .senderId(chatMessage.getSenderType()==SenderType.MEMBER ? chatMessage.getMember().getMemberId() : null)
                .message(chatMessage.getMessage())
                .sentAt(chatMessage.getSentAt())
                .isImage(chatMessage.getIsImage())
                .isPrompt(chatMessage.getIsPrompt())
                .senderType(chatMessage.getSenderType())
                .displayType(chatMessage.getDisplayType())
                .originalMessage(chatMessage.getOriginalMessage())
                .originalSenderName(chatMessage.getOriginalSenderName())
                .build();
    }
}
