package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListUpProjectResponseDTO {

    private Long projectId;

    private String projectName;

    private String projectImage;

    private LocalDate projectCreatedTime;

    private LocalDateTime finalTime;

    private int memberCount;

}
