package com.example.weup.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditFileRequestDTO {
    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private Long fileSize;
    private String filePath;
}