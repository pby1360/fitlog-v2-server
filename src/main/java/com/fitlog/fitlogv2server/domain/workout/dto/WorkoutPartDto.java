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
    // 본인이 만든 부위면 true (공용 부위는 수정/삭제 불가). 목록은 공용 + 본인 부위만 담는다.
    private boolean editable;

    public WorkoutPartDto(WorkoutPart workoutPart) {
        this.id = workoutPart.getId();
        this.name = workoutPart.getName();
        this.editable = workoutPart.getMember() != null;
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        @NotBlank @Size(max = ValidationLimits.MAX_NAME)
        private String name;
    }
}
