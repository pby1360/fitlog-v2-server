package com.fitlog.fitlogv2server.domain.workout.dto;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkoutDto {
    private Long id;
    private String name;
    private String bodyPart;
    private Long bodyPartId;

    public WorkoutDto(Workout workout) {
        this.id = workout.getId();
        this.name = workout.getName();
        this.bodyPart = workout.getWorkoutPart().getName();
        this.bodyPartId = workout.getWorkoutPart().getId();
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String name;
        private Long workoutPartId;
    }
}
