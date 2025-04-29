package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ListUpProjectResponseDTO {

    private Long projectId;

    private String projectName;

    private String projectImage;

    private LocalDate projectCreatedTime;

    private LocalDate projectEndedTime;

    // private int people;

    // private LocalDate lastAccessTime;
}
