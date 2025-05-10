package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditBoardRequestDTO {
    private Long boardId;
    private Long projectId;
    private String title;
    private String contents;
    private String tag;

    private List<Long> remainFileIds;
    private List<EditFileRequestDTO> newFiles;
}
