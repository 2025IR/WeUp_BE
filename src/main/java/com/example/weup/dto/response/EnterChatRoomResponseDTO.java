package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EnterChatRoomResponseDTO {

    private Long memberId;

    private Instant lastReadTime;
}
