package com.fitlog.fitlogv2server.domain.workoutprogram;

import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramExercise;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramPart;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkoutProgramDto {

    public record Request(
            String name,
            String description,
            List<PartDto> parts
    ) {
        public record PartDto(
                Long workoutPartId,
                List<ExerciseDto> exercises
        ) {}

        public record ExerciseDto(
                Long workoutId,
                List<SetDto> sets
        ) {}

        public record SetDto(
                Integer setNumber,
                Double weight,
                Integer reps,
                Integer restTime,
                String memo
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
                        programPart.getExercises().stream().map(ProgramExerciseDto::new).toList());
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
                        programExercise.getSets().stream().map(ProgramSetDto::new).toList());
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
                    program.getCreatedAt().toString().substring(0, 10), // YYYY-MM-DD 형식
                    program.getParts().stream().map(ProgramPartDto::new).toList());
        }
    }
}
