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
public class SendMessageRequestDto {

    private Long projectId;

    private Long senderId;

    private String message;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    private Boolean isImage;
}
