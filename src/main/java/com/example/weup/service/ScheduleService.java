package com.example.weup.service;

import com.example.weup.dto.response.GetScheduleResponseDTO;
import com.example.weup.entity.Member;
import com.example.weup.repository.MemberRepository;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MemberRepository memberRepository;

    private final MemberValidator memberValidator;

    private final ProjectValidator projectValidator;

    public List<GetScheduleResponseDTO> getSchedule(Long userId, Long projectId) {

        projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        List<Member> getMember = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);
        List<GetScheduleResponseDTO> responseDTOList = new ArrayList<>();

        getMember.stream().map(
                member -> GetScheduleResponseDTO.builder()
                        .name(member.getUser().getName())
                        .availableTime(member.getAvailableTime())
                        .isMine(Objects.equals(member.getUser().getUserId(), userId))
                        .build())
                .forEach(responseDTOList::add);

        log.info("get all schedule -> db read success : data size - {}", responseDTOList.size());
        return responseDTOList;
    }

    @Transactional
    public void editSchedule(Long userId, Long projectId, String availableTime) {

        projectValidator.validateActiveProject(projectId);
        Member member = memberValidator.validateActiveMemberInProject(userId, projectId);
        member.editSchedule(availableTime);

        memberRepository.save(member);
        log.info("edit schedule -> db save success : member id - {}", member.getMemberId());
    }

}
