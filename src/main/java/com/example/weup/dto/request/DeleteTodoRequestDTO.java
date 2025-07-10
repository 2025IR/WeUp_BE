package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteTodoRequestDTO {
    private Long projectId;
    private Long todoId;
}
