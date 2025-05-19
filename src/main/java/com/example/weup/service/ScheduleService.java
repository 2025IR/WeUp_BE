package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.EditScheduleRequestDTO;
import com.example.weup.entity.Member;
import com.example.weup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MemberRepository memberRepository;

    @Transactional
    public Map<Long, String> getSchedule(Long projectId) {

        List<Member> getMember = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);

        final int TOTAL_SLOT_COUNT = 48;

        return getMember.stream()
                .collect(Collectors.toMap(
                        Member::getMemberId,
                        member -> {
                            BigInteger data = member.getAvailableTime();
                            return String.format("%" + TOTAL_SLOT_COUNT + "s", data.toString(2))
                                    .replace(' ', '0');
                        }
                ));
    }

    @Transactional
    public void editSchedule(EditScheduleRequestDTO editScheduleRequestDTO) {

        String binaryTime = editScheduleRequestDTO.getAvailableTime();
        BigInteger availableTIme = new BigInteger(binaryTime, 2);

        Member member = memberRepository.findById(editScheduleRequestDTO.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUNT));

        member.setAvailableTime(availableTIme);
    }

}
