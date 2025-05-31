package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.EditScheduleRequestDTO;
import com.example.weup.dto.response.GetScheduleResponseDTO;
import com.example.weup.entity.Member;
import com.example.weup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MemberRepository memberRepository;

    @Transactional
    public List<GetScheduleResponseDTO> getSchedule(Long userId, Long projectId) {

        List<Member> getMember = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);
        List<GetScheduleResponseDTO> responseDTOList = new ArrayList<>();

        getMember.stream().map(
                member -> GetScheduleResponseDTO.builder()
                        .name(member.getUser().getName())
                        .availableTime(member.getAvailableTime())
                        .isMine(Objects.equals(member.getUser().getUserId(), userId))
                        .build()).forEach(responseDTOList::add);

        return responseDTOList;
    }

    @Transactional
    public void editSchedule(Long userId, Long projectId, EditScheduleRequestDTO editScheduleRequestDTO) {

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                        .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

        member.setAvailableTime(editScheduleRequestDTO.getAvailableTime());
    }

}
