package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.CreateChatRoomDTO;
import com.example.weup.dto.request.EditChatRoomNameRequestDTO;
import com.example.weup.dto.request.InviteChatRoomDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetChatRoomListDTO;
import com.example.weup.dto.response.GetInvitableListDTO;
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
    public ResponseEntity<DataResponseDTO<String>> createChatRoom(@LoginUser Long userId,
                                                                  @RequestBody CreateChatRoomDTO createChatRoomDto) throws JsonProcessingException {

        chatRoomService.createChatRoom(userId, createChatRoomDto);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 생성이 완료되었습니다."));
    }

    @PostMapping("/list/member/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<List<GetInvitableListDTO>>> getMemberNotInChatRoom (@LoginUser Long userId,
                                                                                              @PathVariable Long chatRoomId) {

        List<GetInvitableListDTO> getInvitableList = chatRoomService.getMemberNotInChatRoom(chatRoomId);

        return ResponseEntity.ok(DataResponseDTO.of(getInvitableList, "채팅방 초대 가능한 멤버를 불러왔습니다."));
    }

    @PostMapping("/invite/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> inviteChatRoom(@LoginUser Long userId,
                                                                  @PathVariable Long chatRoomId,
                                                                  @RequestBody InviteChatRoomDTO inviteChatRoomDTO) throws JsonProcessingException {

        chatRoomService.inviteChatMember(chatRoomId, inviteChatRoomDTO);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방에 초대되었습니다."));
    }

    @PostMapping("/edit/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> editChatRoom(@LoginUser Long userId,
                                                                @PathVariable Long chatRoomId,
                                                                @RequestBody EditChatRoomNameRequestDTO editChatRoomNameRequestDTO) {

        chatRoomService.editChatRoomName(chatRoomId, editChatRoomNameRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 이름이 수정되었습니다."));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<GetChatRoomListDTO>>> listChatRoom(@LoginUser Long userId,
                                                                                  @PathVariable Long projectId) {

        List<GetChatRoomListDTO> data = chatRoomService.getChatRoomList(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅방 리스트를 불러왔습니다."));
    }

    @PutMapping("/leave/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> exitChatRoom(@LoginUser Long userId,
                                                                @PathVariable Long chatRoomId) throws JsonProcessingException {

        chatRoomService.leaveChatRoom(userId, chatRoomId);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방에서 퇴장하였습니다."));
    }
}
