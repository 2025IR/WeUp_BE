package com.example.weup.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AiGetMessageRequestDTO {

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long chatRoomId;
}
