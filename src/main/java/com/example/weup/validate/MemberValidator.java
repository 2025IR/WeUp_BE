package com.example.weup.validate;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Board;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.repository.MemberRepository;
import com.example.weup.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberRepository memberRepository;

    private final MemberService memberService;

    public Member validateActiveMemberInProject(Long userId, Long projectId) {
        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
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
}
