package com.example.weup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Getter
@Setter
public class EditBoardRequestDTO {
    @NotBlank
    private String title;
    private String contents;
    @NotBlank
    private String tag;
    private List<MultipartFile> file;
    @NotBlank
    private List<Long> removeFileIds;
}
