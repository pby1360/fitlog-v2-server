package com.fitlog.fitlogv2server.domain.workoutprogram.facade;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.service.WorkoutProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public void updateWorkoutProgram(Long memberId, Long programId, WorkoutProgramDto.Request requestDto) {
        Member member = memberService.findMemberById(memberId);
        workoutProgramService.updateWorkoutProgram(programId, requestDto, member);
    }

    @Transactional
    public void deleteWorkoutProgram(Long memberId, Long programId) {
        Member member = memberService.findMemberById(memberId);
        workoutProgramService.deleteWorkoutProgram(programId, member);
    }

    @Transactional(readOnly = true)
    public List<WorkoutProgramDto.Response> findAllPrograms(Long memberId) {
        return workoutProgramService.findAllPrograms(memberId);
    }
}
