package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class CreateTodoRequestDTO {
    private Long projectId;
}

