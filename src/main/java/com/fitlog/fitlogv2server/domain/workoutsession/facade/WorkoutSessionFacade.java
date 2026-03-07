package com.fitlog.fitlogv2server.domain.workoutsession.facade;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.service.WorkoutSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        WorkoutSession workoutSession = workoutSessionService.startSession(member, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSessionDto.Response> getLatestInProgressSession(Long memberId) {
        return workoutSessionService.getLatestInProgressSession(memberId)
                .map(WorkoutSessionDto.Response::new);
    }

    @Transactional
    public WorkoutSessionDto.Response completeSet(Long memberId, Long sessionId, WorkoutSessionDto.CompleteSetRequest request) {
        WorkoutSession workoutSession = workoutSessionService.completeSet(memberId, sessionId, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response pauseSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = workoutSessionService.pauseSession(memberId, sessionId);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response resumeSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = workoutSessionService.resumeSession(memberId, sessionId);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response endSession(Long memberId, Long sessionId, WorkoutSessionDto.EndRequest request) {
        WorkoutSession workoutSession = workoutSessionService.endSession(memberId, sessionId, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response skipExercise(Long memberId, Long sessionId, WorkoutSessionDto.SkipExerciseRequest request) {
        WorkoutSession workoutSession = workoutSessionService.skipExercise(memberId, sessionId, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response reorderExercises(Long memberId, Long sessionId, WorkoutSessionDto.ReorderExercisesRequest request) {
        WorkoutSession workoutSession = workoutSessionService.reorderExercises(memberId, sessionId, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response addExercise(Long memberId, Long sessionId, WorkoutSessionDto.AddExerciseRequest request) {
        WorkoutSession workoutSession = workoutSessionService.addExercise(memberId, sessionId, request);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional
    public WorkoutSessionDto.Response removeExercise(Long memberId, Long sessionId, Long workoutSessionExerciseId) {
        WorkoutSession workoutSession = workoutSessionService.removeExercise(memberId, sessionId, workoutSessionExerciseId);
        return new WorkoutSessionDto.Response(workoutSession);
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDto.LogPageResponse getWorkoutLog(Long memberId, Pageable pageable) {
        Page<WorkoutSessionDto.LogSummaryResponse> page = workoutSessionService.getCompletedSessions(memberId, pageable)
                .map(WorkoutSessionDto.LogSummaryResponse::new);
        long totalDurationSeconds = workoutSessionService.sumDurationSeconds(memberId);
        long totalCompletedSets = workoutSessionService.sumCompletedSets(memberId);
        long totalSets = workoutSessionService.sumTotalSets(memberId);
        return new WorkoutSessionDto.LogPageResponse(page, totalDurationSeconds, totalCompletedSets, totalSets);
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDto.Response getSessionDetail(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = workoutSessionService.getSessionDetail(memberId, sessionId);
        return new WorkoutSessionDto.Response(workoutSession);
    }
}
