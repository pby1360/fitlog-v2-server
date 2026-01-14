package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Integer restTime;
    private String memo;
    private Boolean completed;

    @Builder
    public WorkoutSessionSet(WorkoutSessionExercise workoutSessionExercise, Integer setNumber, Double weight, Integer reps, Integer restTime, String memo, Boolean completed) {
        this.workoutSessionExercise = workoutSessionExercise;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
        this.restTime = restTime;
        this.memo = memo;
        this.completed = completed;
    }
}
