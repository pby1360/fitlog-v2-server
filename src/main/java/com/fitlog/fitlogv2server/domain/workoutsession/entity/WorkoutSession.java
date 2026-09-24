package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fitlog.fitlogv2server.global.exception.ConflictException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_program_id")
    private WorkoutProgram workoutProgram;

    // 세션 생성 시점의 프로그램 이름 (이후 프로그램 이름이 바뀌어도 기록 표기는 유지)
    private String programName;

    private ZonedDateTime startTime;

    private ZonedDateTime endTime;

    private ZonedDateTime lastPausedAt;

    private Long totalPausedSeconds = 0L;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkoutSessionExercise> workoutSessionExercises = new HashSet<>();

    @Builder
    public WorkoutSession(Member member, WorkoutProgram workoutProgram, ZonedDateTime startTime, ZonedDateTime endTime, SessionStatus status) {
        this.member = member;
        this.workoutProgram = workoutProgram;
        this.programName = workoutProgram != null ? workoutProgram.getName() : null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalPausedSeconds = 0L;
    }

    public void addWorkoutSessionExercise(WorkoutSessionExercise workoutSessionExercise) {
        this.workoutSessionExercises.add(workoutSessionExercise);
    }

    // --- 상태 전이 규칙 ---
    // IN_PROGRESS <-> PAUSED 는 서로 전환 가능, 두 상태 모두에서 COMPLETED/CANCELLED 로 종료할 수 있다.
    // 종료된 세션(COMPLETED/CANCELLED)은 더 이상 변경할 수 없다.

    public boolean isActive() {
        return this.status == SessionStatus.IN_PROGRESS || this.status == SessionStatus.PAUSED;
    }

    public void assertModifiable() {
        if (!isActive()) {
            throw new ConflictException("종료되거나 취소된 세션은 변경할 수 없습니다.");
        }
    }

    // 이미 일시정지 상태면 그대로 둔다 (재시도 요청에 안전)
    public void pause(ZonedDateTime pausedAt) {
        assertModifiable();
        if (this.status == SessionStatus.IN_PROGRESS) {
            this.status = SessionStatus.PAUSED;
            this.lastPausedAt = pausedAt;
        }
    }

    // 이미 진행 중이면 그대로 둔다 (재시도 요청에 안전)
    public void resume(ZonedDateTime resumedAt) {
        assertModifiable();
        if (this.status == SessionStatus.PAUSED) {
            settlePause(resumedAt);
            this.status = SessionStatus.IN_PROGRESS;
        }
    }

    /**
     * 세션 종료. 종료 시각은 서버 시각을 사용한다.
     * 이미 같은 상태로 종료된 세션에 대한 재요청은 무시하고, 다른 상태로 바꾸려는 요청은 거부한다.
     */
    public void finish(SessionStatus targetStatus, ZonedDateTime endedAt) {
        if (targetStatus != SessionStatus.COMPLETED && targetStatus != SessionStatus.CANCELLED) {
            throw new IllegalArgumentException("세션 종료 상태는 COMPLETED 또는 CANCELLED 만 가능합니다.");
        }
        if (!isActive()) {
            if (this.status == targetStatus) {
                return;
            }
            throw new ConflictException("이미 " + this.status + " 상태로 종료된 세션입니다.");
        }
        // 일시정지 중에 종료하면 마지막 정지 구간도 운동 시간에서 제외한다
        settlePause(endedAt);
        this.status = targetStatus;
        this.endTime = endedAt;
    }

    private void settlePause(ZonedDateTime until) {
        if (this.lastPausedAt != null) {
            long pausedSeconds = Math.max(0, Duration.between(this.lastPausedAt, until).getSeconds());
            this.totalPausedSeconds = (this.totalPausedSeconds == null ? 0L : this.totalPausedSeconds) + pausedSeconds;
            this.lastPausedAt = null;
        }
    }

    /**
     * 실제 운동 시간(초) = 종료 - 시작 - 총 일시정지. 종료되지 않은 세션은 null.
     */
    // 스냅샷이 없는 과거 데이터는 현재 프로그램 이름으로 대체한다
    public String getDisplayProgramName() {
        if (this.programName != null) return this.programName;
        return this.workoutProgram != null ? this.workoutProgram.getName() : null;
    }

    public Long getDurationSeconds() {
        if (this.startTime == null || this.endTime == null) {
            return null;
        }
        long paused = this.totalPausedSeconds != null ? this.totalPausedSeconds : 0L;
        return Math.max(0, Duration.between(this.startTime, this.endTime).getSeconds() - paused);
    }

    public boolean isAllSetsCompleted() {
        return this.workoutSessionExercises.stream()
                .filter(exercise -> !exercise.getSkipped())
                .flatMap(exercise -> exercise.getWorkoutSessionSets().stream())
                .allMatch(set -> Boolean.TRUE.equals(set.getCompleted()));
    }

    public void removeWorkoutSessionExercise(WorkoutSessionExercise exercise) {
        this.workoutSessionExercises.remove(exercise);
    }

}
