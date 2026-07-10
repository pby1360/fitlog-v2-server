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

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final WorkoutSessionExerciseRepository workoutSessionExerciseRepository;
    private final WorkoutSessionSetRepository workoutSessionSetRepository;
    private final WorkoutRepository workoutRepository;

    @Transactional
    public WorkoutSession startSession(Member member, WorkoutSessionDto.StartRequest request) {
        WorkoutProgram workoutProgram = workoutProgramRepository.findById(request.getWorkoutProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Workout program not found"));

        if (workoutProgram.isDeleted()) {
            throw new IllegalArgumentException("Cannot start a session with a deleted workout program.");
        }

        WorkoutSession workoutSession = WorkoutSession.builder()
                .member(member)
                .workoutProgram(workoutProgram)
                .startTime(ZonedDateTime.now(ZoneOffset.UTC))
                .status(SessionStatus.IN_PROGRESS)
                .build();

        List<WorkoutSessionDto.CustomExerciseRequest> customExercises = request.getCustomExercises();
        if (customExercises != null && !customExercises.isEmpty()) {
            for (WorkoutSessionDto.CustomExerciseRequest customEx : customExercises) {
                Workout workout = workoutRepository.findById(customEx.getWorkoutId())
                        .orElseThrow(() -> new IllegalArgumentException("Workout not found: " + customEx.getWorkoutId()));

                WorkoutSessionExercise sessionExercise = WorkoutSessionExercise.builder()
                        .workoutSession(workoutSession)
                        .workout(workout)
                        .order(customEx.getOrder())
                        .build();
                workoutSession.addWorkoutSessionExercise(sessionExercise);

                if (customEx.getSets() != null) {
                    for (WorkoutSessionDto.CustomSetRequest setReq : customEx.getSets()) {
                        WorkoutSessionSet sessionSet = WorkoutSessionSet.builder()
                                .workoutSessionExercise(sessionExercise)
                                .setNumber(setReq.getSetNumber())
                                .weight(setReq.getWeight())
                                .reps(setReq.getReps())
                                .restTime(setReq.getRestTime())
                                .memo(setReq.getMemo())
                                .completed(false)
                                .build();
                        sessionExercise.addWorkoutSessionSet(sessionSet);
                    }
                }
            }
        } else {
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
    public Page<WorkoutSession> getCompletedSessions(Long memberId, Pageable pageable, ZonedDateTime startDate, ZonedDateTime endDate) {
        return workoutSessionRepository.findAllByMemberIdAndStatusBetween(memberId, SessionStatus.COMPLETED, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public long sumDurationSeconds(Long memberId) {
        Long result = workoutSessionRepository.sumDurationSecondsByMemberId(memberId);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long sumDurationSeconds(Long memberId, ZonedDateTime startDate, ZonedDateTime endDate) {
        Long result = workoutSessionRepository.sumDurationSecondsByMemberIdBetween(memberId, startDate, endDate);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long sumCompletedSets(Long memberId) {
        Long result = workoutSessionRepository.sumCompletedSetsByMemberId(memberId);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long sumCompletedSets(Long memberId, ZonedDateTime startDate, ZonedDateTime endDate) {
        Long result = workoutSessionRepository.sumCompletedSetsByMemberIdBetween(memberId, startDate, endDate);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long sumTotalSets(Long memberId) {
        Long result = workoutSessionRepository.sumTotalSetsByMemberId(memberId);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long sumTotalSets(Long memberId, ZonedDateTime startDate, ZonedDateTime endDate) {
        Long result = workoutSessionRepository.sumTotalSetsByMemberIdBetween(memberId, startDate, endDate);
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public WorkoutSession getSessionDetail(Long memberId, Long sessionId) {
        return workoutSessionRepository.findDetailByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found or does not belong to the member"));
    }

    @Transactional
    public WorkoutSession skipExercise(Long memberId, Long sessionId, WorkoutSessionDto.SkipExerciseRequest request) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);

        WorkoutSessionExercise exercise = workoutSession.getWorkoutSessionExercises().stream()
                .filter(e -> e.getId().equals(request.getWorkoutSessionExerciseId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session exercise not found"));

        if (Boolean.TRUE.equals(request.getSkipped())) {
            exercise.skip();
        } else {
            exercise.unskip();
        }

        if (workoutSession.isAllSetsCompleted()) {
            workoutSession.updateStatusAndEndTime(SessionStatus.COMPLETED, ZonedDateTime.now(ZoneOffset.UTC));
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession reorderExercises(Long memberId, Long sessionId, WorkoutSessionDto.ReorderExercisesRequest request) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);

        Map<Long, Integer> orderMap = request.getExercises().stream()
                .collect(Collectors.toMap(
                        WorkoutSessionDto.ExerciseOrderItem::getWorkoutSessionExerciseId,
                        WorkoutSessionDto.ExerciseOrderItem::getOrder
                ));

        for (WorkoutSessionExercise exercise : workoutSession.getWorkoutSessionExercises()) {
            Integer newOrder = orderMap.get(exercise.getId());
            if (newOrder != null) {
                exercise.updateOrder(newOrder);
            }
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession addExercise(Long memberId, Long sessionId, WorkoutSessionDto.AddExerciseRequest request) {
        WorkoutSession workoutSession = findOwnedSession(sessionId, memberId);
        validateSessionModifiable(workoutSession);

        if (request.getSets() == null || request.getSets().isEmpty()) {
            throw new IllegalArgumentException("sets는 최소 1개 이상이어야 합니다.");
        }

        Workout workout = workoutRepository.findById(request.getWorkoutId())
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        // Shift existing exercises' order to keep ordering consistent
        int newOrder = request.getOrder() != null ? request.getOrder() :
                workoutSession.getWorkoutSessionExercises().size() + 1;

        for (WorkoutSessionExercise existing : workoutSession.getWorkoutSessionExercises()) {
            if (existing.getOrder() >= newOrder) {
                existing.updateOrder(existing.getOrder() + 1);
            }
        }

        WorkoutSessionExercise sessionExercise = WorkoutSessionExercise.builder()
                .workoutSession(workoutSession)
                .workout(workout)
                .order(newOrder)
                .build();
        workoutSession.addWorkoutSessionExercise(sessionExercise);

        int setNumber = 1;
        for (WorkoutSessionDto.AddSetRequest setRequest : request.getSets()) {
            WorkoutSessionSet sessionSet = WorkoutSessionSet.builder()
                    .workoutSessionExercise(sessionExercise)
                    .setNumber(setNumber++)
                    .weight(setRequest.getWeight())
                    .reps(setRequest.getReps())
                    .restTime(setRequest.getRestTime())
                    .memo(setRequest.getMemo())
                    .completed(false)
                    .actualWeight(null)
                    .actualReps(null)
                    .actualMemo(null)
                    .completedAt(null)
                    .build();
            sessionExercise.addWorkoutSessionSet(sessionSet);
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession removeExercise(Long memberId, Long sessionId, Long workoutSessionExerciseId) {
        WorkoutSession workoutSession = findWorkoutSessionByIdAndMemberId(sessionId, memberId);

        WorkoutSessionExercise exerciseToRemove = workoutSession.getWorkoutSessionExercises().stream()
                .filter(e -> e.getId().equals(workoutSessionExerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session exercise not found"));

        int removedOrder = exerciseToRemove.getOrder();
        workoutSession.removeWorkoutSessionExercise(exerciseToRemove);

        // Re-order remaining exercises
        for (WorkoutSessionExercise exercise : workoutSession.getWorkoutSessionExercises()) {
            if (exercise.getOrder() > removedOrder) {
                exercise.updateOrder(exercise.getOrder() - 1);
            }
        }

        return workoutSession;
    }

    @Transactional
    public WorkoutSession startExercise(Long memberId, Long sessionId, Long exerciseId, ZonedDateTime startedAt) {
        WorkoutSession workoutSession = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found"));

        if (!workoutSession.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (startedAt.isBefore(workoutSession.getStartTime())) {
            throw new IllegalArgumentException("startedAt cannot be before session startTime");
        }

        WorkoutSessionExercise exercise = workoutSession.getWorkoutSessionExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session exercise not found in this session"));

        exercise.updateStartedAt(startedAt);
        return workoutSession;
    }

    @Transactional
    public WorkoutSession addSet(Long memberId, Long sessionId, Long workoutSessionExerciseId, WorkoutSessionDto.CreateSetRequest request) {
        WorkoutSession workoutSession = findOwnedSession(sessionId, memberId);
        validateSessionModifiable(workoutSession);

        if (request.getReps() == null) {
            throw new IllegalArgumentException("reps는 필수입니다.");
        }
        if (request.getReps() <= 0) {
            throw new IllegalArgumentException("reps는 1 이상이어야 합니다.");
        }
        if (request.getRestTime() == null) {
            throw new IllegalArgumentException("restTime은 필수입니다.");
        }
        if (request.getRestTime() < 0) {
            throw new IllegalArgumentException("restTime은 0 이상이어야 합니다.");
        }
        if (request.getWeight() != null && request.getWeight() < 0) {
            throw new IllegalArgumentException("weight는 0 이상이어야 합니다.");
        }

        WorkoutSessionExercise exercise = workoutSession.getWorkoutSessionExercises().stream()
                .filter(e -> e.getId().equals(workoutSessionExerciseId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session exercise not found in this session"));

        int nextSetNumber = exercise.getWorkoutSessionSets().stream()
                .mapToInt(WorkoutSessionSet::getSetNumber)
                .max()
                .orElse(0) + 1;

        WorkoutSessionSet newSet = WorkoutSessionSet.builder()
                .workoutSessionExercise(exercise)
                .setNumber(nextSetNumber)
                .weight(request.getWeight())
                .reps(request.getReps())
                .restTime(request.getRestTime())
                .memo(request.getMemo())
                .completed(false)
                .actualWeight(null)
                .actualReps(null)
                .actualMemo(null)
                .completedAt(null)
                .build();

        workoutSessionSetRepository.save(newSet);
        exercise.addWorkoutSessionSet(newSet);

        return workoutSession;
    }

    private WorkoutSession findWorkoutSessionByIdAndMemberId(Long sessionId, Long memberId) {
        return workoutSessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Workout session not found or does not belong to the member"));
    }

    private WorkoutSession findOwnedSession(Long sessionId, Long memberId) {
        WorkoutSession workoutSession = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
        if (!workoutSession.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return workoutSession;
    }

    private void validateSessionModifiable(WorkoutSession workoutSession) {
        SessionStatus status = workoutSession.getStatus();
        if (status == SessionStatus.COMPLETED || status == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "종료되거나 취소된 세션에는 운동/세트를 추가할 수 없습니다.");
        }
    }
}
