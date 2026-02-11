package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private ZonedDateTime startTime;

    private ZonedDateTime endTime;

    private ZonedDateTime lastPausedAt;

    private Long totalPausedSeconds = 0L;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL)
    private Set<WorkoutSessionExercise> workoutSessionExercises = new HashSet<>();

    @Builder
    public WorkoutSession(Member member, WorkoutProgram workoutProgram, ZonedDateTime startTime, ZonedDateTime endTime, SessionStatus status) {
        this.member = member;
        this.workoutProgram = workoutProgram;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalPausedSeconds = 0L;
    }

    public void addWorkoutSessionExercise(WorkoutSessionExercise workoutSessionExercise) {
        this.workoutSessionExercises.add(workoutSessionExercise);
    }

    public void pause(ZonedDateTime pausedAt) {
        if (this.status == SessionStatus.IN_PROGRESS) {
            this.status = SessionStatus.PAUSED;
            this.lastPausedAt = pausedAt;
        }
    }

    public void resume(ZonedDateTime resumedAt) {
        if (this.status == SessionStatus.PAUSED && this.lastPausedAt != null) {
            long pausedSeconds = java.time.Duration.between(this.lastPausedAt, resumedAt).getSeconds();
            this.totalPausedSeconds = (this.totalPausedSeconds == null ? 0L : this.totalPausedSeconds) + pausedSeconds;
            this.status = SessionStatus.IN_PROGRESS;
            this.lastPausedAt = null;
        }
    }

    public void updateStatus(SessionStatus status) {
        this.status = status;
    }

    public void updateEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public void updateStatusAndEndTime(SessionStatus status, ZonedDateTime endTime) {
        this.status = status;
        this.endTime = endTime;
    }

    public boolean isAllSetsCompleted() {
        return this.workoutSessionExercises.stream()
                .flatMap(exercise -> exercise.getWorkoutSessionSets().stream())
                .allMatch(WorkoutSessionSet::getCompleted);
    }

}
