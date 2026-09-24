package com.fitlog.fitlogv2server.domain.workout.dto;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.global.common.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @NotBlank @Size(max = ValidationLimits.MAX_NAME)
        private String name;
        @NotNull
        private Long workoutPartId;
    }
}
