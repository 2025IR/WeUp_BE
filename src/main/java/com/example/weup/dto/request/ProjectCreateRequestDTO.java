package com.example.weup.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProjectCreateRequestDTO {

    private String projectName;

    private MultipartFile projectImage;

}