package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class EditTodoRequestDTO {
    private Long projectId;
    private Long todoId;
    private String todoName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> memberIds;
}