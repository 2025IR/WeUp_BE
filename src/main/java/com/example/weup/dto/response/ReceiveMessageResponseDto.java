package com.example.weup.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ReceiveMessageResponseDto {

    private Long senderId;

    private String senderName;

    private String senderProfileImage;

    private String message;

    private LocalDateTime sentAt;

    @JsonProperty("isImage")
    private boolean isImage;
}
