package com.example.weup.dto.response;

import com.example.weup.constant.DisplayType;
import com.example.weup.constant.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisMessageDTO {

    private Long chatRoomId;

    private Long memberId;

    private String message;

    private Boolean isImage;

    private LocalDateTime sentAt;

    @Builder.Default
    private SenderType senderType = SenderType.MEMBER;

    @Builder.Default
    private DisplayType displayType = DisplayType.DEFAULT;
}
