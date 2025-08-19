package com.example.weup.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReceiveMessageToConnectResponseDTO {

    private String message;

    private LocalDateTime sentAt;
}
