package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DetailProjectResponseDTO {

    private String projectName;

    private String projectImage;

    private String description;

    private LocalDate projectCreatedTime;

    private boolean status;

    private boolean isRevealedNumber;

    private Long memberId;

    private boolean isLeader;
}
