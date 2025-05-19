package com.example.weup.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EditScheduleRequestDTO {

    private Long memberId;

    private String availableTime;
}
