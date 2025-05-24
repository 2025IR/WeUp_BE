package com.example.weup.dto.request;

import lombok.Getter;

@Getter
public class BoardListRequestDTO {
    private String tag;
    private String search;
    private int page = 0;
    private int size = 10;
}
