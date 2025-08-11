package com.example.weup.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatMessageResponseDTO {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String message;

    private Boolean isImage;

    private LocalDateTime sentAt;
}
