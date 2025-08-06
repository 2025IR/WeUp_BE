package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatPageResponseDto {

    private List<ReceiveMessageResponseDTO> messageList;

    private int page;

    private boolean isLastPage;
}
