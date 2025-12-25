package com.fitlog.fitlogv2server.domain.workoutprogram.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutProgramSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_program_exercise_id")
    private WorkoutProgramExercise workoutProgramExercise;

    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Integer restTime;
    private String memo;

    @Builder
    public WorkoutProgramSet(WorkoutProgramExercise workoutProgramExercise, Integer setNumber, Double weight, Integer reps, Integer restTime, String memo) {
        this.workoutProgramExercise = workoutProgramExercise;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
        this.restTime = restTime;
        this.memo = memo;
    }
}
