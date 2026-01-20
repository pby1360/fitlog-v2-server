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

import java.time.LocalDateTime;
import java.util.Optional;

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
                .startTime(LocalDateTime.now())
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
        return workoutSessionRepository.findLatestInProgressSessionByMemberId(memberId, SessionStatus.IN_PROGRESS);
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
            workoutSession.updateStatusAndEndTime(SessionStatus.COMPLETED, LocalDateTime.now());
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession pauseSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        if (workoutSession.getStatus() == SessionStatus.PAUSED) {
            throw new IllegalStateException("Workout session is already paused.");
        }
        workoutSession.updateStatus(SessionStatus.PAUSED);
        return workoutSession;
    }

    @Transactional
    public WorkoutSession resumeSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        if (workoutSession.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Workout session is already in progress.");
        }
        workoutSession.updateStatus(SessionStatus.IN_PROGRESS);
        return workoutSession;
    }

    @Transactional
    public WorkoutSession endSession(Long memberId, Long sessionId, WorkoutSessionDto.EndRequest request) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);
        workoutSession.updateStatusAndEndTime(request.getStatus(), request.getEndTime());
        return workoutSession;
    }

    private WorkoutSession findWorkoutSessionByIdAndMemberId(Long sessionId, Long memberId) {
        return workoutSessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found or does not belong to the member"));
    }
}
