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
public class GetChatRoomListDTO {

    private Long chatRoomId;

    private Long chatRoomMemberId;

    private String chatRoomName;

    private List<String> chatRoomMemberNames;

    private Boolean isBasic;
}
