package com.example.weup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class AiGetMessagesForMinutesRequestDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long chatRoomId;
}
