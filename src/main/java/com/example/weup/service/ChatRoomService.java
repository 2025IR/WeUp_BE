package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.CreateChatRoomDTO;
import com.example.weup.dto.request.InviteChatRoomDTO;
import com.example.weup.dto.response.GetChatRoomListDTO;
import com.example.weup.dto.response.GetInvitableListDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.ChatMessageRepository;
import com.example.weup.repository.ChatRoomMemberRepository;
import com.example.weup.repository.ChatRoomRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatService chatService;

    private final ChatRoomRepository chatRoomRepository;

    private final ProjectValidator projectValidator;

    private final MemberValidator memberValidator;

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final MemberRepository memberRepository;

    private final S3Service s3Service;

    private final ChatMessageRepository chatMessageRepository;

    private final StringRedisTemplate redisTemplate;

    public ChatRoom createBasicChatRoom(Project project, String projectName) {

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(projectName + " 채팅방")
                .project(project)
                .basic(true)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    public void createChatRoom(User user, CreateChatRoomDTO createChatRoomDto) {

        Project project = projectValidator.validateActiveProject(createChatRoomDto.getProjectId());
        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), project.getProjectId());

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(createChatRoomDto.getChatRoomName())
                .project(project)
                .basic(true)
                .build();

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .chatRoom(chatRoom)
                .build();

        chatRoomRepository.save(chatRoom);
        chatRoomMemberRepository.save(chatRoomMember);
    }

    public List<GetInvitableListDTO> getMemberNotInChatRoom(Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));
        Project project = projectValidator.validateActiveProject(chatRoom.getProject().getProjectId());

        List<Member> allProjectMember = memberRepository.findByProject(project);
        Set<Member> memberInChatRoom = chatRoomMemberRepository.findByChatRoom(chatRoom).stream()
                .map(ChatRoomMember::getMember)
                .collect(Collectors.toSet());

        return allProjectMember.stream()
                .filter(member -> !memberInChatRoom.contains(member))
                .map(member -> GetInvitableListDTO.builder()
                        .memberId(member.getMemberId())
                        .memberName(member.getUser().getName())
                        .profileImage(s3Service.getPresignedUrl(member.getUser().getProfileImage()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void inviteChatMember(Long chatRoomId, InviteChatRoomDTO inviteChatRoomDTO) throws JsonProcessingException {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));
        Project project = projectValidator.validateActiveProject(chatRoom.getProject().getProjectId());

        for (Long memberId : inviteChatRoomDTO.getInviteMemberIds()) {
            addChatRoomMember(project, chatRoom, memberId);
        }
    }

    public void addChatRoomMember(Project project, ChatRoom chatRoom, Long memberId) throws JsonProcessingException {

        Member member = memberValidator.validateMember(project.getProjectId(), memberId);
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, member, false);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .chatRoom(chatRoom)
                .build();

        chatService.saveSystemMessage(chatRoom.getChatRoomId(), member.getUser().getName() + "님이 채팅방에 참여했습니다.");

        chatRoomMemberRepository.save(chatRoomMember);
    }

    @Transactional
    public void editChatRoomName(Long chatRoomId, String chatRoomName) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        chatRoom.editChatRoomName(chatRoomName);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public List<GetChatRoomListDTO> getChatRoomList(User user, Long projectId) {

        Project project = projectValidator.validateActiveProject(projectId);
        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), project.getProjectId());

        return chatRoomRepository.findByProject(project).stream()
                .sorted(Comparator
                        .comparing(ChatRoom::isBasic, Comparator.reverseOrder())
                        .thenComparing(ChatRoom::getCreatedAt, Comparator.reverseOrder())
                )
                .map(chatRoom -> {
                    ChatRoomMember targetChatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);

                    List<String> chatRoomMemberNames = chatRoomMemberRepository.findByChatRoom(chatRoom).stream()
                            .map(chatRoomMember -> chatRoomMember.getMember().getUser().getName())
                            .collect(Collectors.toList());

                    return GetChatRoomListDTO.builder()
                            .chatRoomId(chatRoom.getChatRoomId())
                            .chatRoomMemberId(targetChatRoomMember.getChatRoomMemberId())
                            .chatRoomName(chatRoom.getChatRoomName())
                            .chatRoomMemberNames(chatRoomMemberNames)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void leaveChatRoom(User user, Long chatRoomId) throws JsonProcessingException {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), chatRoom.getProject().getProjectId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, member, true);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        chatRoomMemberRepository.delete(chatRoomMember);

        chatService.saveSystemMessage(chatRoomId, member.getUser().getName() + "님이 채팅방에서 퇴장했습니다.");

        List<ChatRoomMember> remainingMembers = chatRoomMemberRepository.findByChatRoom(chatRoom);
        if (remainingMembers.isEmpty()) {
            String key = "chat:room:" + chatRoomId;

            redisTemplate.delete(key);
            chatMessageRepository.deleteByChatRoom(chatRoom);
            chatRoomRepository.delete(chatRoom);

            log.info("chat room deleted -> roomId: {}", chatRoomId);
        }
    }

}
