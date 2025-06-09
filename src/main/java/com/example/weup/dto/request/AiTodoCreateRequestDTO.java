package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AiTodoCreateRequestDTO {

    private Long projectId;

    private String todoName;

    private LocalDate startDate;

    //private Long memberId;
}
