package com.example.weup.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class BoardCreateRequestDTO {
    private Long projectId;
    @NotBlank(message = "제목을 입력해주세요.")
    private String title;
    private String contents;
    @NotBlank(message = "태그를 설정해주세요.")
    private String tag;
    private List<MultipartFile> file;

    @AssertTrue(message = "파일이나 내용을 입력해주세요.")
    public boolean isContentsOrFilePresent() {
        boolean hasContents = (contents != null && !contents.trim().isEmpty());
        boolean hasFile = (file != null && !file.isEmpty());
        return hasContents || hasFile;
    }
}

