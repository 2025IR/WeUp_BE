package com.example.weup.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class ProjectEditRequestDTO {

    private String projectName;

    private MultipartFile projectImage;

    private boolean status;

    private boolean revealedNumber;
}
