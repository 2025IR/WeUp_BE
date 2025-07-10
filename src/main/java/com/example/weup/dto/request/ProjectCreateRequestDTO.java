package com.example.weup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProjectCreateRequestDTO {

    @NotBlank(message = "프로젝트 이름을 입력해주세요.")
    private String projectName;

    private MultipartFile projectImage;

}