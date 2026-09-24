package com.fitlog.fitlogv2server.domain.workout.dto;

import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.global.common.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkoutPartDto {
    private Long id;
    private String name;

    public WorkoutPartDto(WorkoutPart workoutPart) {
        this.id = workoutPart.getId();
        this.name = workoutPart.getName();
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        @NotBlank @Size(max = ValidationLimits.MAX_NAME)
        private String name;
    }
}
