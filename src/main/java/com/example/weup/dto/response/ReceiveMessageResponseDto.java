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

    private String originalSenderName;

    private String originalMessage;

    public static ReceiveMessageResponseDto fromEntity(ChatMessage chatMessage) {
        return ReceiveMessageResponseDto.builder()
                .senderId(chatMessage.getMessageId())
                .senderName(chatMessage.getSenderType() == SenderType.MEMBER
                        ? chatMessage.getMember().getUser().getName()
                        : null)
                .message(chatMessage.getMessage())
                .sentAt(chatMessage.getSentAt())
                .isImage(chatMessage.getIsImage())
                .senderType(chatMessage.getSenderType())
                .displayType(chatMessage.getDisplayType())
                .build();
    }

    public ReceiveMessageResponseDtoBuilder copyBuilder() {
        return builder()
                .senderId(this.senderId)
                .senderName(this.senderName)
                .senderProfileImage(this.senderProfileImage)
                .message(this.message)
                .sentAt(this.sentAt)
                .senderType(this.senderType)
                .isImage(this.isImage)
                .displayType(this.displayType);
    }
}
