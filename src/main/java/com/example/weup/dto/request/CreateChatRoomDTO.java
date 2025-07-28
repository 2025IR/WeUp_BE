package com.example.weup.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateChatRoomDTO {

    private String chatRoomName;

    private Long projectId;

    public List<Long> chatRoomMemberId;
}
