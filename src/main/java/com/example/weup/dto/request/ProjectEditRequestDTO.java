package com.example.weup.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class ProjectEditRequestDTO {

    @NotNull(message = "프로젝트 이름이 없습니다.")
    private String projectName;

    private MultipartFile projectImage;

    @NotNull(message = "프로젝트 상태 데이터가 없습니다.")
    private boolean status;

    @NotNull(message = "프로젝트 전화번호 공개 여부 데이터가 없습니다.")
    private boolean revealedNumber;
}
