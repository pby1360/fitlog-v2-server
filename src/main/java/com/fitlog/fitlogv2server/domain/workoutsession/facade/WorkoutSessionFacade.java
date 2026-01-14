package com.fitlog.fitlogv2server.domain.workoutsession.facade;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.service.WorkoutSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkoutSessionFacade {
    private final WorkoutSessionService workoutSessionService;
    private final MemberService memberService;

    @Transactional
    public WorkoutSessionDto.Response startSession(Long memberId, WorkoutSessionDto.StartRequest request) {
        Member member = memberService.findMemberById(memberId);
        WorkoutSession workoutSession = workoutSessionService.startSession(member, request.getWorkoutProgramId());
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSessionDto.Response> getLatestInProgressSession(Long memberId) {
        return workoutSessionService.getLatestInProgressSession(memberId)
                .map(WorkoutSessionDto.Response::new);
    }
}
