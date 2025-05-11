package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class BoardListResponseDTO {
    private Long boardId;
    private String nickname;
    private String profileImage;
    private String title;
    private LocalDateTime boardCreatedTime;
    private String tag;
    private boolean hasFile;
}
