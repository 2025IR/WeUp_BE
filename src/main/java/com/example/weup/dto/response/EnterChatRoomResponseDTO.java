package com.example.weup.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public class EnterChatRoomResponseDTO {

    private Long memberId;

    private Instant lastReadTime;
}
