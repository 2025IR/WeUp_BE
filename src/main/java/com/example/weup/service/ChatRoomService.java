package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.CreateChatRoomDTO;
import com.example.weup.dto.request.EditChatRoomNameRequestDTO;
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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final SessionService sessionService;

    public ChatRoom createBasicChatRoom(Project project, String projectName) {

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(projectName + " 채팅방")
                .project(project)
                .basic(true)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void createChatRoom(Long userId, CreateChatRoomDTO createChatRoomDto) throws JsonProcessingException {

        Project project = projectValidator.validateActiveProject(createChatRoomDto.getProjectId());
        Member creator = memberValidator.validateActiveMemberInProject(userId, project.getProjectId());

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(createChatRoomDto.getChatRoomName())
                .project(project)
                .basic(false)
                .build();

        chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성 완료, Chat Room ID : {}", chatRoom.getChatRoomId());

        addChatRoomMember(chatRoom, creator.getMemberId());

        List<Long> chatRoomMemberIds = createChatRoomDto.getChatRoomMemberId();
        if (chatRoomMemberIds != null && !createChatRoomDto.getChatRoomMemberId().isEmpty()) {
            for (Long memberId : chatRoomMemberIds) {
                log.info("초대할 대상 Member ID : {}", memberId);
                addChatRoomMember(chatRoom, memberId);
            }
        }
    }

    public List<GetInvitableListDTO> getMemberNotInChatRoom(Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));
        projectValidator.validateActiveProject(chatRoom.getProject().getProjectId());

        List<Member> allProjectMember = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(chatRoom.getProject().getProjectId());
        Set<Member> memberInChatRoom = chatRoomMemberRepository.findByChatRoom(chatRoom).stream()
                .map(ChatRoomMember::getMember)
                .collect(Collectors.toSet());

        log.info("전체 프로젝트 멤버 : {} 명", allProjectMember.size());
        allProjectMember.forEach(member -> log.info("All Project Member ID : {}", member.getMemberId()));

        log.info("채팅방에 있는 멤버 : {} 명", memberInChatRoom.size());
        memberInChatRoom.forEach(member -> log.info("Member In Chat Room Id : {}", member.getMemberId()));

        return allProjectMember.stream()
                .filter(member -> {
                    boolean isNotInChatRoom = !memberInChatRoom.contains(member);
                    if (!isNotInChatRoom) {
                        log.info("Filtering out Member ID: {} (already in chat room)", member.getMemberId());
                    }
                    return isNotInChatRoom;
                })
                .peek(member -> log.info("Passed filter -> Member ID: {}", member.getMemberId()))
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

        for (Long memberId : inviteChatRoomDTO.getInviteMemberIds()) {
            addChatRoomMember(chatRoom, memberId);
        }
    }

    @Transactional
    public void addChatRoomMember(ChatRoom chatRoom, Long memberId) throws JsonProcessingException {

        Member member = memberValidator.validateMemberAndProject(memberId);
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, member, false);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .chatRoom(chatRoom)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);
        log.info("채팅방 멤버 추가, Chat Room Member ID : {}, Chat Room ID : {}, Member ID : {}", chatRoomMember.getChatRoomMemberId(), chatRoom.getChatRoomId(), memberId);
        chatService.sendSystemMessage(chatRoom.getChatRoomId(), member.getUser().getName() + "님이 채팅방에 참여했습니다.");
        log.info("Add Chat Room Member 로직 끝");
    }

    @Transactional
    public void editChatRoomName(Long chatRoomId, EditChatRoomNameRequestDTO editChatRoomNameRequestDTO) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        chatRoom.editChatRoomName(editChatRoomNameRequestDTO.getChatRoomName());
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public List<GetChatRoomListDTO> getChatRoomList(Long userId, Long projectId) {

        Project project = projectValidator.validateActiveProject(projectId);
        Member member = memberValidator.validateActiveMemberInProject(userId, project.getProjectId());

        List<ChatRoomMember> chatRoomMembers = chatRoomMemberRepository.findByMember(member);

        return chatRoomMembers.stream()
                .map(ChatRoomMember::getChatRoom)
                .sorted(Comparator
                        .comparing(ChatRoom::isBasic, Comparator.reverseOrder())
                        .thenComparing(ChatRoom::getCreatedAt, Comparator.reverseOrder())
                )
                .map(chatRoom -> {
                    ChatRoomMember targetChatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);

                    List<String> chatRoomMemberNames = chatRoomMemberRepository.findByChatRoom(chatRoom).stream()
                            .map(chatRoomMember -> chatRoomMember.getMember().getUser().getName())
                            .collect(Collectors.toList());

                    long unreadMessageCount = getUnreadMessageCount(chatRoom.getChatRoomId(), userId);

                    return GetChatRoomListDTO.builder()
                            .chatRoomId(chatRoom.getChatRoomId())
                            .chatRoomMemberId(targetChatRoomMember.getChatRoomMemberId())
                            .chatRoomName(chatRoom.getChatRoomName())
                            .chatRoomMemberNames(chatRoomMemberNames)
                            .isBasic(chatRoom.isBasic())
                            .unreadMessageCount(unreadMessageCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private long getUnreadMessageCount(Long chatRoomId, Long userId) {
        Instant lastReadTime = sessionService.getLastReadAt(chatRoomId, userId);
        long redisUnreadCount = 0L;
        long mysqlUnreadCount = 0L;

        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        String redisKey = "chat:room:" + chatRoomId;

        Long redisCount = zSetOperations.count(redisKey, lastReadTime.toEpochMilli(), Double.POSITIVE_INFINITY);
        if (redisCount != null) {
            redisUnreadCount = redisCount;
        }

        LocalDateTime lastDateTime = LocalDateTime.ofInstant(lastReadTime, ZoneId.systemDefault());
        mysqlUnreadCount = chatMessageRepository.countByChatRoom_ChatRoomIdAndSentAtAfter(chatRoomId, lastDateTime);

        return redisUnreadCount + mysqlUnreadCount;
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) throws JsonProcessingException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member member = memberValidator.validateActiveMemberInProject(userId, chatRoom.getProject().getProjectId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, member, true);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        chatRoomMemberRepository.delete(chatRoomMember);

        chatService.sendSystemMessage(chatRoomId, member.getUser().getName() + "님이 채팅방에서 퇴장했습니다.");

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
