package com.fitlog.fitlogv2server.domain.workoutsession.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramExercise;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramPart;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramSet;
import com.fitlog.fitlogv2server.domain.workoutprogram.repository.WorkoutProgramRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionExerciseRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final WorkoutSessionExerciseRepository workoutSessionExerciseRepository;
    private final WorkoutSessionSetRepository workoutSessionSetRepository;

    @Transactional
    public WorkoutSession startSession(Member member, Long workoutProgramId) {
        WorkoutProgram workoutProgram = workoutProgramRepository.findById(workoutProgramId)
                .orElseThrow(() -> new IllegalArgumentException("Workout program not found"));

        WorkoutSession workoutSession = WorkoutSession.builder()
                .member(member)
                .workoutProgram(workoutProgram)
                .startTime(ZonedDateTime.now(ZoneOffset.UTC))
                .status(SessionStatus.IN_PROGRESS)
                .build();

        int orderCounter = 1;
        for (WorkoutProgramPart programPart : workoutProgram.getParts()) {
            for (WorkoutProgramExercise programExercise : programPart.getExercises()) {
                WorkoutSessionExercise sessionExercise = WorkoutSessionExercise.builder()
                        .workoutSession(workoutSession)
                        .workout(programExercise.getWorkout())
                        .order(orderCounter++)
                        .build();
                workoutSession.addWorkoutSessionExercise(sessionExercise);

                for (WorkoutProgramSet programSet : programExercise.getSets()) {
                    WorkoutSessionSet sessionSet = WorkoutSessionSet.builder()
                            .workoutSessionExercise(sessionExercise)
                            .setNumber(programSet.getSetNumber())
                            .weight(programSet.getWeight())
                            .reps(programSet.getReps())
                            .restTime(programSet.getRestTime())
                            .memo(programSet.getMemo())
                            .completed(false)
                            .actualWeight(null)
                            .actualReps(null)
                            .actualMemo(null)
                            .build();
                    sessionExercise.addWorkoutSessionSet(sessionSet);
                }
            }
        }

        return workoutSessionRepository.save(workoutSession);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getLatestInProgressSession(Long memberId) {
        return workoutSessionRepository.findLatestWorkoutSessionByMemberIdAndStatuses(memberId, Set.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED));
    }

    @Transactional
    public WorkoutSession completeSet(Long memberId, Long sessionId, WorkoutSessionDto.CompleteSetRequest request) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);

        WorkoutSessionSet workoutSessionSet = workoutSession.getWorkoutSessionExercises().stream()
                .filter(exercise -> exercise.getId().equals(request.getWorkoutSessionExerciseId()))
                .flatMap(exercise -> exercise.getWorkoutSessionSets().stream())
                .filter(set -> set.getId().equals(request.getWorkoutSessionSetId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session set not found"));

        workoutSessionSet.completeSet(request.getActualWeight(), request.getActualReps(), request.getMemo());

        if (workoutSession.isAllSetsCompleted()) {
            workoutSession.updateStatusAndEndTime(SessionStatus.COMPLETED, ZonedDateTime.now(ZoneOffset.UTC));
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession pauseSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        if (workoutSession.getStatus() == SessionStatus.PAUSED) {
            throw new IllegalStateException("Workout session is already paused.");
        }
        workoutSession.pause(ZonedDateTime.now(ZoneOffset.UTC));
        return workoutSession;
    }

    @Transactional
    public WorkoutSession resumeSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        if (workoutSession.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Workout session is already in progress.");
        }
        workoutSession.resume(ZonedDateTime.now(ZoneOffset.UTC));
        return workoutSession;
    }

    @Transactional
    public WorkoutSession endSession(Long memberId, Long sessionId, WorkoutSessionDto.EndRequest request) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        workoutSession.updateStatusAndEndTime(request.getStatus(), request.getEndTime());
        return workoutSession;
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSession> getCompletedSessions(Long memberId, Pageable pageable) {
        return workoutSessionRepository.findAllByMemberIdAndStatus(memberId, SessionStatus.COMPLETED, pageable);
    }

    @Transactional(readOnly = true)
    public WorkoutSession getSessionDetail(Long memberId, Long sessionId) {
        return workoutSessionRepository.findDetailByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found or does not belong to the member"));
    }

    private WorkoutSession findWorkoutSessionByIdAndMemberId(Long sessionId, Long memberId) {
        return workoutSessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found or does not belong to the member"));
    }
}
