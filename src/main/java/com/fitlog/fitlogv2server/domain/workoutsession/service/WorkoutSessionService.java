package com.fitlog.fitlogv2server.domain.workoutsession.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
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
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionSetRepository;
import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    // 사용자당 동시에 하나만 존재할 수 있는 활성 상태
    private static final Set<SessionStatus> ACTIVE_STATUSES = Set.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED);
    // 운동 시작 시각을 기록할 때 허용하는 기기-서버 시계 오차
    private static final long CLOCK_SKEW_TOLERANCE_SECONDS = 300;

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final WorkoutSessionSetRepository workoutSessionSetRepository;
    private final WorkoutRepository workoutRepository;

    @Transactional
    public WorkoutSession startSession(Member member, WorkoutSessionDto.StartRequest request) {
        // 사용자당 활성 세션은 1개. 동시 요청은 DB 부분 유니크 인덱스(V3)가 최종적으로 막는다.
        if (workoutSessionRepository.existsByMemberIdAndStatusIn(member.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("이미 진행 중인 운동이 있습니다.");
        }

        // 본인 소유 프로그램만 허용 (다른 회원의 템플릿을 복사해 오는 IDOR 방지)
        WorkoutProgram workoutProgram = workoutProgramRepository.findByIdAndMemberId(request.getWorkoutProgramId(), member.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workout program not found"));

        if (workoutProgram.isDeleted()) {
            throw new IllegalArgumentException("Cannot start a session with a deleted workout program.");
        }

        WorkoutSession workoutSession = WorkoutSession.builder()
                .member(member)
                .workoutProgram(workoutProgram)
                .startTime(now())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        List<WorkoutSessionDto.CustomExerciseRequest> customExercises = request.getCustomExercises();
        if (customExercises != null && !customExercises.isEmpty()) {
            for (WorkoutSessionDto.CustomExerciseRequest customEx : customExercises) {
                Workout workout = workoutRepository.findAccessibleById(customEx.getWorkoutId(), member.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Workout not found: " + customEx.getWorkoutId()));

                WorkoutSessionExercise sessionExercise = WorkoutSessionExercise.builder()
                        .workoutSession(workoutSession)
                        .workout(workout)
                        .order(customEx.getOrder())
                        .build();
                workoutSession.addWorkoutSessionExercise(sessionExercise);

                // 세트 번호는 요청 값이 아니라 서버가 1부터 순서대로 부여한다 (중복 번호 방지)
                int setNumber = 1;
                List<WorkoutSessionDto.CustomSetRequest> sets = customEx.getSets() != null ? customEx.getSets() : List.of();
                for (WorkoutSessionDto.CustomSetRequest setReq : sets) {
                    sessionExercise.addWorkoutSessionSet(newSet(sessionExercise, setNumber++,
                            setReq.getWeight(), setReq.getReps(), setReq.getRestTime(), setReq.getMemo()));
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

                    int setNumber = 1;
                    for (WorkoutProgramSet programSet : programExercise.getSets()) {
                        sessionExercise.addWorkoutSessionSet(newSet(sessionExercise, setNumber++,
                                programSet.getWeight(), programSet.getReps(), programSet.getRestTime(), programSet.getMemo()));
                    }
                }
            }
        }

        return workoutSessionRepository.save(workoutSession);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getLatestInProgressSession(Long memberId) {
        // 활성 세션 ID를 1건만 찾은 뒤 상세를 읽는다 (컬렉션 fetch join 결과가 여러 행이어도 안전)
        return workoutSessionRepository.findFirstByMemberIdAndStatusInOrderByIdDesc(memberId, ACTIVE_STATUSES)
                .flatMap(session -> workoutSessionRepository.findDetailByIdAndMemberId(session.getId(), memberId));
    }

    @Transactional
    public WorkoutSession completeSet(Long memberId, Long sessionId, WorkoutSessionDto.CompleteSetRequest request) {
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);

        WorkoutSessionSet workoutSessionSet = workoutSession.getWorkoutSessionExercises().stream()
                .filter(exercise -> exercise.getId().equals(request.getWorkoutSessionExerciseId()))
                .flatMap(exercise -> exercise.getWorkoutSessionSets().stream())
                .filter(set -> set.getId().equals(request.getWorkoutSessionSetId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session set not found"));

        workoutSessionSet.completeSet(request.getActualWeight(), request.getActualReps(), request.getMemo());
        completeIfAllSetsDone(workoutSession);

        return workoutSession;
    }

    @Transactional
    public WorkoutSession pauseSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findOwnedSessionForUpdate(sessionId, memberId);
        workoutSession.pause(now());
        return workoutSession;
    }

    @Transactional
    public WorkoutSession resumeSession(Long memberId, Long sessionId) {
        WorkoutSession workoutSession = findOwnedSessionForUpdate(sessionId, memberId);
        workoutSession.resume(now());
        return workoutSession;
    }

    /**
     * 세션 종료. 요청의 종료 시각은 무시하고 서버 시각으로 기록한다.
     */
    @Transactional
    public WorkoutSession endSession(Long memberId, Long sessionId, WorkoutSessionDto.EndRequest request) {
        WorkoutSession workoutSession = findOwnedSessionForUpdate(sessionId, memberId);
        workoutSession.finish(request.getStatus(), now());
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
    }

    @Transactional
    public WorkoutSession skipExercise(Long memberId, Long sessionId, WorkoutSessionDto.SkipExerciseRequest request) {
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);
        WorkoutSessionExercise exercise = findExercise(workoutSession, request.getWorkoutSessionExerciseId());

        if (Boolean.TRUE.equals(request.getSkipped())) {
            exercise.skip();
        } else {
            exercise.unskip();
        }

        completeIfAllSetsDone(workoutSession);
        return workoutSession;
    }

    @Transactional
    public WorkoutSession reorderExercises(Long memberId, Long sessionId, WorkoutSessionDto.ReorderExercisesRequest request) {
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);

        Map<Long, Integer> orderMap = request.getExercises().stream()
                .collect(Collectors.toMap(
                        WorkoutSessionDto.ExerciseOrderItem::getWorkoutSessionExerciseId,
                        WorkoutSessionDto.ExerciseOrderItem::getOrder,
                        (first, duplicate) -> {
                            throw new IllegalArgumentException("같은 운동이 중복 지정되었습니다.");
                        }
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
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);

        if (request.getSets() == null || request.getSets().isEmpty()) {
            throw new IllegalArgumentException("sets는 최소 1개 이상이어야 합니다.");
        }

        Workout workout = workoutRepository.findAccessibleById(request.getWorkoutId(), memberId)
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
            sessionExercise.addWorkoutSessionSet(newSet(sessionExercise, setNumber++,
                    setRequest.getWeight(), setRequest.getReps(), setRequest.getRestTime(), setRequest.getMemo()));
        }

        return workoutSession;
    }

    /**
     * 세션에서 운동을 제거한다. 부모 컬렉션의 orphanRemoval 로 운동과 하위 세트가 DB에서도 삭제된다.
     */
    @Transactional
    public WorkoutSession removeExercise(Long memberId, Long sessionId, Long workoutSessionExerciseId) {
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);
        WorkoutSessionExercise exerciseToRemove = findExercise(workoutSession, workoutSessionExerciseId);

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
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);

        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt은 필수입니다.");
        }
        if (startedAt.isBefore(workoutSession.getStartTime())) {
            throw new IllegalArgumentException("startedAt cannot be before session startTime");
        }
        if (startedAt.isAfter(now().plusSeconds(CLOCK_SKEW_TOLERANCE_SECONDS))) {
            throw new IllegalArgumentException("startedAt cannot be in the future");
        }

        WorkoutSessionExercise exercise = findExercise(workoutSession, exerciseId);
        exercise.updateStartedAt(startedAt);
        return workoutSession;
    }

    @Transactional
    public WorkoutSession addSet(Long memberId, Long sessionId, Long workoutSessionExerciseId, WorkoutSessionDto.CreateSetRequest request) {
        WorkoutSession workoutSession = findModifiableSession(sessionId, memberId);

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

        // 세션 행 잠금 안에서 계산하므로 동시 요청이 같은 번호를 받지 않는다 (UNIQUE 제약이 최종 방어)
        int nextSetNumber = exercise.getWorkoutSessionSets().stream()
                .mapToInt(WorkoutSessionSet::getSetNumber)
                .max()
                .orElse(0) + 1;

        WorkoutSessionSet newSet = newSet(exercise, nextSetNumber,
                request.getWeight(), request.getReps(), request.getRestTime(), request.getMemo());

        workoutSessionSetRepository.save(newSet);
        exercise.addWorkoutSessionSet(newSet);

        return workoutSession;
    }

    private void completeIfAllSetsDone(WorkoutSession workoutSession) {
        if (workoutSession.isAllSetsCompleted()) {
            workoutSession.finish(SessionStatus.COMPLETED, now());
        }
    }

    private WorkoutSessionExercise findExercise(WorkoutSession workoutSession, Long exerciseId) {
        return workoutSession.getWorkoutSessionExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout session exercise not found"));
    }

    private WorkoutSessionSet newSet(WorkoutSessionExercise exercise, int setNumber,
                                     Double weight, Integer reps, Integer restTime, String memo) {
        return WorkoutSessionSet.builder()
                .workoutSessionExercise(exercise)
                .setNumber(setNumber)
                .weight(weight)
                .reps(reps)
                .restTime(restTime)
                .memo(memo)
                .completed(false)
                .build();
    }

    /**
     * 본인 세션을 행 잠금과 함께 조회한다.
     * 없는 세션과 다른 회원의 세션은 구분하지 않고 404로 응답한다 (존재 여부 노출 방지).
     */
    private WorkoutSession findOwnedSessionForUpdate(Long sessionId, Long memberId) {
        return workoutSessionRepository.findByIdForUpdate(sessionId)
                .filter(session -> session.getMember().getId().equals(memberId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
    }

    // 종료/취소되지 않은 본인 세션만 반환한다 (종료된 세션 변경 시 409)
    private WorkoutSession findModifiableSession(Long sessionId, Long memberId) {
        WorkoutSession workoutSession = findOwnedSessionForUpdate(sessionId, memberId);
        workoutSession.assertModifiable();
        return workoutSession;
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(AppTimeZone.KST);
    }
}
