package com.example.weup.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SendMessageRequestDTO {

    @NotNull(message = "메시지를 보내는 사람이 없습니다.")
    private Long senderId;

    private String message;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @Builder.Default
    @JsonProperty("isImage")
    private Boolean isImage = false;
}
