package com.fitlog.fitlogv2server.domain.workout.facade;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutFacade {

    private final WorkoutService workoutService;
    private final MemberService memberService;

    public void addWorkoutPart(WorkoutPartDto.Request request, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.addWorkoutPart(request, member);
    }

    public void updateWorkoutPart(Long workoutPartId, WorkoutPartDto.Request request, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.updateWorkoutPart(workoutPartId, request, member);
    }

    public void deleteWorkoutPart(Long workoutPartId, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.deleteWorkoutPart(workoutPartId, member);
    }

    public void addWorkout(WorkoutDto.Request request, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.addWorkout(request, member);
    }

    public void updateWorkout(Long workoutId, WorkoutDto.Request request, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.updateWorkout(workoutId, request, member);
    }

    public void deleteWorkout(Long workoutId, Long memberId) {
        Member member = memberService.findMemberById(memberId);
        workoutService.deleteWorkout(workoutId, member);
    }
}
