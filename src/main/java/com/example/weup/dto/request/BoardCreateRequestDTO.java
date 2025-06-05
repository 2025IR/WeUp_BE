package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class BoardCreateRequestDTO {
    private String title;
    private String contents;
    private String tag;
    private List<MultipartFile> file;
}

