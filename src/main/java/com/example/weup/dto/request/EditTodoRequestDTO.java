package com.example.weup.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditTodoRequestDTO {
    private Long todoId;
    private String todoName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> memberIds;
}