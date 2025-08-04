package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.CreateChatRoomDTO;
import com.example.weup.dto.request.InviteChatRoomDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetChatRoomListDTO;
import com.example.weup.dto.response.GetInvitableListDTO;
import com.example.weup.entity.User;
import com.example.weup.service.ChatRoomService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<String>> createChatRoom(@LoginUser User user,
                                                                  @RequestBody CreateChatRoomDTO createChatRoomDto) {

        chatRoomService.createChatRoom(user, createChatRoomDto);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 생성이 완료되었습니다."));
    }

    @PostMapping("/list/member/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<List<GetInvitableListDTO>>> getMemberNotInChatRoom (@LoginUser User user,
                                                                                              @PathVariable Long chatRoomId) {

        List<GetInvitableListDTO> getInvitableList = chatRoomService.getMemberNotInChatRoom(chatRoomId);

        return ResponseEntity.ok(DataResponseDTO.of(getInvitableList, "채팅방 초대 가능한 멤버를 불러왔습니다."));
    }

    @PostMapping("/invite/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> inviteChatRoom(@LoginUser User user,
                                                                  @PathVariable Long chatRoomId,
                                                                  @RequestBody InviteChatRoomDTO inviteChatRoomDTO) throws JsonProcessingException {

        chatRoomService.inviteChatMember(chatRoomId, inviteChatRoomDTO);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방에 초대되었습니다."));
    }

    @PostMapping("/edit/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> editChatRoom(@LoginUser User user,
                                                                @PathVariable Long chatRoomId,
                                                                @RequestBody String chatRoomName) {

        chatRoomService.editChatRoomName(chatRoomId, chatRoomName);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 이름이 수정되었습니다."));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<GetChatRoomListDTO>>> listChatRoom(@LoginUser User user,
                                                                                  @PathVariable Long projectId) {

        List<GetChatRoomListDTO> data = chatRoomService.getChatRoomList(user, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅방 리스트를 불러왔습니다."));
    }

    @DeleteMapping("/leave/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> exitChatRoom(@LoginUser User user,
                                                                @PathVariable Long chatRoomId) throws JsonProcessingException {

        chatRoomService.leaveChatRoom(user, chatRoomId);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방에서 퇴장하였습니다."));
    }
}
