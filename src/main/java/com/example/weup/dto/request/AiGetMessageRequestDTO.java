package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class AiGetMessageRequestDTO {

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long chatRoomId;
}
