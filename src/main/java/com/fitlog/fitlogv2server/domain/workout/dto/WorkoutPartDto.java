package com.fitlog.fitlogv2server.domain.workout.dto;

import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import lombok.Data;

@Data
public class WorkoutPartDto {
    private Long id;
    private String name;

    public WorkoutPartDto(WorkoutPart workoutPart) {
        this.id = workoutPart.getId();
        this.name = workoutPart.getName();
    }
}
