package com.example.weup.dto.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditTodoStatusRequestDTO {
    private Long todoId;
    private Byte status;
}
