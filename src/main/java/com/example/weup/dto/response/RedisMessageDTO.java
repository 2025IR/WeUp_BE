package com.example.weup.dto.response;

import com.example.weup.constant.DisplayType;
import com.example.weup.constant.SenderType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisMessageDTO {

    private Long chatRoomId;

    private String uuid;

    private Long memberId;

    private String message;

    private Boolean isImage;

    private LocalDateTime sentAt;

    @Builder.Default
    private SenderType senderType = SenderType.MEMBER;

    @Builder.Default
    private DisplayType displayType = DisplayType.DEFAULT;

    private String originalSenderName;

    private String originalMessage;
}
