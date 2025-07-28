package com.example.weup.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class InviteChatRoomDTO {

    private Long projectId;

    private List<Long> inviteMemberIds;
}
