package com.fitlog.fitlogv2server.domain.workoutprogram;

import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramExercise;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramPart;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramSet;
import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.global.common.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkoutProgramDto {

    public record Request(
            @NotBlank @Size(max = ValidationLimits.MAX_NAME) String name,
            @Size(max = ValidationLimits.MAX_DESCRIPTION) String description,
            @NotNull @Size(max = ValidationLimits.MAX_PARTS) List<@Valid PartDto> parts
    ) {
        public record PartDto(
                @NotNull Long workoutPartId,
                @NotNull @Size(max = ValidationLimits.MAX_EXERCISES) List<@Valid ExerciseDto> exercises
        ) {}

        public record ExerciseDto(
                @NotNull Long workoutId,
                @NotNull @Size(max = ValidationLimits.MAX_SETS) List<@Valid SetDto> sets
        ) {}

        public record SetDto(
                @NotNull @Min(1) Integer setNumber,
                @DecimalMin("0") @DecimalMax(ValidationLimits.MAX_WEIGHT) Double weight,
                @NotNull @Min(0) @Max(ValidationLimits.MAX_REPS) Integer reps,
                @NotNull @Min(0) @Max(ValidationLimits.MAX_REST_SECONDS) Integer restTime,
                @Size(max = ValidationLimits.MAX_MEMO) String memo
        ) {}
    }

    public record Response(
            Long id,
            String name,
            String description,
            String createdAt,
            List<ProgramPartDto> parts
    ) {
        public record ProgramPartDto(
                Long id,
                Long workoutPartId,
                String workoutPartName,
                Integer order,
                List<ProgramExerciseDto> exercises
        ) {
            public ProgramPartDto(WorkoutProgramPart programPart) {
                this(programPart.getId(), programPart.getWorkoutPart().getId(), programPart.getWorkoutPart().getName(), programPart.getOrder(),
                        programPart.getExercises().stream()
                                .sorted(Comparator.comparingInt(WorkoutProgramExercise::getOrder))
                                .map(ProgramExerciseDto::new).toList());
            }
        }

        public record ProgramExerciseDto(
                Long id,
                Long workoutId,
                String workoutName,
                String workoutPartName,
                Integer order,
                List<ProgramSetDto> sets
        ) {
            public ProgramExerciseDto(WorkoutProgramExercise programExercise) {
                this(programExercise.getId(), programExercise.getWorkout().getId(), programExercise.getWorkout().getName(),
                        programExercise.getWorkout().getWorkoutPart().getName(), programExercise.getOrder(),
                        programExercise.getSets().stream()
                                .sorted(Comparator.comparing(WorkoutProgramSet::getSetNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(ProgramSetDto::new).toList());
            }
        }

        public record ProgramSetDto(
                Long id,
                Integer setNumber,
                Double weight,
                Integer reps,
                Integer restTime,
                String memo
        ) {
            public ProgramSetDto(WorkoutProgramSet programSet) {
                this(programSet.getId(), programSet.getSetNumber(), programSet.getWeight(), programSet.getReps(), programSet.getRestTime(), programSet.getMemo());
            }
        }

        public Response(WorkoutProgram program) {
            this(program.getId(), program.getName(), program.getDescription(),
                    AppTimeZone.toKstDateString(program.getCreatedAt()), // 생성일(한국 날짜, YYYY-MM-DD)
                    program.getParts().stream()
                            .sorted(Comparator.comparingInt(WorkoutProgramPart::getOrder))
                            .map(ProgramPartDto::new).toList());
        }
    }
}
