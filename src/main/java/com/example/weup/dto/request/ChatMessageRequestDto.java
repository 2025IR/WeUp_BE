package com.example.weup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChatMessageRequestDto {

    private Long projectId;

    private Long senderId;

    private String message;

    private Boolean isImage;

    private LocalDateTime sentAt;
}
