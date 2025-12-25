package com.fitlog.fitlogv2server.domain.workoutprogram;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkoutProgramDto {

    public record Request(
            String name,
            String description,
            List<WorkoutPartDto> workoutParts
    ) {
        public record WorkoutPartDto(
                String name,
                List<WorkoutExerciseDto> workoutExercises
        ) {}

        public record WorkoutExerciseDto(
                String name,
                List<WorkoutSetDto> workoutSets
        ) {}

        public record WorkoutSetDto(
                Integer setNumber,
                Double weight,
                Integer reps,
                Integer restTime,
                String memo
        ) {}
    }
}
