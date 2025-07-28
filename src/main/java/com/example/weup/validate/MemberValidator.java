package com.example.weup.validate;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Board;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.repository.ChatRoomMemberRepository;
import com.example.weup.repository.ChatRoomRepository;
import com.example.weup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public Member validateActiveMemberInProject(Long userId, Long projectId) {
        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        return member;
    }

    public void validateBoardWriter(Board board, Member member) {
        if (!board.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.NOT_WRITER);
        }
    }

    public void isLeader(Long userId, Project project) {
        Member targetMember = validateActiveMemberInProject(userId, project.getProjectId());

        if(!targetMember.isLeader()) {
            throw new GeneralException(ErrorInfo.NOT_LEADER);
        }
    }

    public boolean isDeletedMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::isMemberDeleted)
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));
    }

    public Member validateMemberInChatRoom(Long projectId, Long memberId, Long chatRoomId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        if (!member.getProject().getProjectId().equals(projectId)) {
            throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
        }

        if (isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        if (chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new GeneralException(ErrorInfo.NOT_LEADER);
        }

        return member;
    }

    public Member validateMember(Long projectId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

        if (!member.getProject().getProjectId().equals(projectId)) {
            throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
        }

        if (isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        return member;
    }

    public void isMemberAlreadyInChatRoom(ChatRoom chatRoom, Member member, boolean targetResult) {

        if (targetResult) {
            if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
                throw new GeneralException(ErrorInfo.MEMBER_NOT_FOUND);
            }
        }

        if (!targetResult) {
            if (chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
                throw new GeneralException(ErrorInfo.MEMBER_ALREADY_EXISTS_IN_CHAT_ROOM);
            }
        }
    }
}
