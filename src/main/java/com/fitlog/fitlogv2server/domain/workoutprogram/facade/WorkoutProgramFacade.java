package com.fitlog.fitlogv2server.domain.workoutprogram.facade;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.service.WorkoutProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkoutProgramFacade {

    private final WorkoutProgramService workoutProgramService;
    private final MemberService memberService;

    @Transactional
    public void createWorkoutProgram(Long memberId, WorkoutProgramDto.Request requestDto) {
        Member member = memberService.findMemberById(memberId);
        workoutProgramService.createWorkoutProgram(requestDto, member);
    }
}
