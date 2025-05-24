package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileFullResponseDTO {
    private String originalFileName;
    private String storedFileName;
    private Long fileSize;
    private String fileType;
}
