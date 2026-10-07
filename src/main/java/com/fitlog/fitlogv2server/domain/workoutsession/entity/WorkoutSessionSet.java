package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_exercise_id")
    private WorkoutSessionExercise workoutSessionExercise;

    @Column(nullable = false)
    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Integer restTime;
    private String memo;
    @Column(nullable = false)
    private Boolean completed;
    private Double actualWeight;
    private Integer actualReps;
    private String actualMemo;
    private ZonedDateTime completedAt;

    @Builder
    public WorkoutSessionSet(WorkoutSessionExercise workoutSessionExercise, Integer setNumber, Double weight, Integer reps, Integer restTime, String memo, Boolean completed, Double actualWeight, Integer actualReps, String actualMemo, ZonedDateTime completedAt) {
        this.workoutSessionExercise = workoutSessionExercise;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
        this.restTime = restTime;
        this.memo = memo;
        this.completed = completed;
        this.actualWeight = actualWeight;
        this.actualReps = actualReps;
        this.actualMemo = actualMemo;
        this.completedAt = completedAt;
    }

    /**
     * 세트 완료. 이미 완료된 세트에 같은 값이 다시 오면(재시도) 아무것도 바꾸지 않고,
     * 다른 값이 오면 기존 기록을 덮어쓰지 않도록 거부한다.
     */
    public void completeSet(Double actualWeight, Integer actualReps, String actualMemo) {
        if (Boolean.TRUE.equals(this.completed)) {
            boolean sameRequest = Objects.equals(this.actualWeight, actualWeight)
                    && Objects.equals(this.actualReps, actualReps)
                    && Objects.equals(this.actualMemo, actualMemo);
            if (sameRequest) {
                return;
            }
            throw new ConflictException("이미 완료된 세트입니다.");
        }
        this.actualWeight = actualWeight;
        this.actualReps = actualReps;
        this.actualMemo = actualMemo;
        this.completed = true;
        this.completedAt = ZonedDateTime.now(AppTimeZone.KST);
    }
}
