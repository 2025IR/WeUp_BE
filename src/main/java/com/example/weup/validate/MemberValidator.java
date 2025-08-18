package com.example.weup.validate;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Board;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.repository.ChatRoomMemberRepository;
import com.example.weup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberRepository memberRepository;

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final ProjectValidator projectValidator;

    public Member validateActiveMemberInProject(Long userId, Long projectId) {
        log.debug("member validator - validate active member in project : {}", projectId);
        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        log.debug("member validator -> end");
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

    public Member validateMemberAndProject(Long memberId) {
        log.debug("member validate, validation member and project - memberId: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

        projectValidator.validateActiveProject(member.getProject().getProjectId());

        if (isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        return member;
    }

    public void isMemberAlreadyInChatRoom(ChatRoom chatRoom, Member member, boolean targetResult) {
        log.debug("member validate, is member already in chat room? - memberId: {}", member.getMemberId());
        if (targetResult) {
            if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
                throw new GeneralException(ErrorInfo.MEMBER_NOT_FOUND);
            }
        } else {
            if (chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
                throw new GeneralException(ErrorInfo.MEMBER_ALREADY_EXISTS_IN_CHAT_ROOM);
            }
        }
    }

}
