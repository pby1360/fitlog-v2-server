package com.fitlog.fitlogv2server.domain.workout.dto;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import lombok.Data;

@Data
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
}
