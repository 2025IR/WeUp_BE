package com.example.weup.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TodoListResponseDTO {
    private Long todoId;
    private String todoName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Byte status;
    @JsonProperty("isMyTodo")
    private boolean isMyTodo;
    private List<TodoAssigneeResponseDTO> assignee;
}
