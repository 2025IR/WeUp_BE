package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DetailProjectResponseDTO {

    private String projectName;

    private String projectImage;

    private String description;

    private boolean status;

    private boolean isRevealedNumber;

    private boolean isLeader;
}
