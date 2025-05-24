package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EditTodoStatusRequestDTO {
    private Long todoId;
    private Byte status;
}
