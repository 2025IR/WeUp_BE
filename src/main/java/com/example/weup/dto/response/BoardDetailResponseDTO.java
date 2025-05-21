package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class BoardDetailResponseDTO {
    private String name;
    private String profileImage;
    private String title;
    private String contents;
    private LocalDateTime boardCreatedTime;
    private String tag;
    private List<FileResponseDTO> files;
}
