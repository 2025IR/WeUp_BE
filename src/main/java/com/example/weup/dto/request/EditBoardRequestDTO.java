package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


@Getter
@Setter
public class EditBoardRequestDTO {
    private Long boardId;
    private Long projectId;
    private String title;
    private String contents;
    private String tag;
    private MultipartFile file;
}
