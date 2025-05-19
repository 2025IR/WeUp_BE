package com.example.weup.service;

import com.example.weup.entity.Member;
import com.example.weup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MemberRepository memberRepository;

    @Transactional
    public List<> getSchedule(Long projectId) {
        List<Member> getMember = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);

        return getMember.stream()
                .map(member -> new Member)
    }

}
