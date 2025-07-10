package com.example.weup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Getter
@Setter
public class EditBoardRequestDTO {
    @NotBlank(message = "제목은 필수로 입력되어야 합니다.")
    private String title;
    private String contents;
    @NotBlank(message = "태그는 필수로 입력되어야 합니다.")
    private String tag;
    private List<MultipartFile> file;
    private List<Long> removeFileIds;
}
