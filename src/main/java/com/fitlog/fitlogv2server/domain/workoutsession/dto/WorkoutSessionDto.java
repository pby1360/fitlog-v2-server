package com.fitlog.fitlogv2server.domain.workoutsession.dto;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class WorkoutSessionDto {

    @Getter
    public static class StartRequest {
        private Long workoutProgramId;
    }

    @Getter
    public static class CompleteSetRequest {
        private Long workoutSessionExerciseId;
        private Long workoutSessionSetId;
        private Double actualWeight;
        private Integer actualReps;
        private String memo;
    }

    @Getter
    public static class EndRequest {
        private ZonedDateTime endTime;
        private SessionStatus status;
    }

    @Getter
    public static class Response {
        private Long id;
        private Long workoutProgramId;
        private String workoutProgramName;
        private ZonedDateTime startTime;
        private String status;
        private List<ExerciseResponse> exercises;

        public Response(WorkoutSession workoutSession) {
            this.id = workoutSession.getId();
            this.workoutProgramId = workoutSession.getWorkoutProgram().getId();
            this.workoutProgramName = workoutSession.getWorkoutProgram().getName();
            this.startTime = workoutSession.getStartTime();
            this.status = workoutSession.getStatus().name();
            this.exercises = workoutSession.getWorkoutSessionExercises().stream()
                    .sorted(Comparator.comparing(WorkoutSessionExercise::getOrder))
                    .map(ExerciseResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    public static class ExerciseResponse {
        private Long id;
        private Long workoutId;
        private String workoutName;
        private int order;
        private List<SetResponse> sets;

        public ExerciseResponse(WorkoutSessionExercise exercise) {
            this.id = exercise.getId();
            this.workoutId = exercise.getWorkout().getId();
            this.workoutName = exercise.getWorkout().getName();
            this.order = exercise.getOrder();
            this.sets = exercise.getWorkoutSessionSets().stream()
                    .sorted(Comparator.comparing(WorkoutSessionSet::getSetNumber))
                    .map(SetResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    public static class SetResponse {
        private Long id;
        private int setNumber;
        private Double weight;
        private int reps;
        private int restTime;
        private String memo;
        private boolean completed;
        private Double actualWeight;
        private Integer actualReps;
        private String actualMemo;
        private java.time.LocalDateTime completedAt;

        public SetResponse(WorkoutSessionSet set) {
            this.id = set.getId();
            this.setNumber = set.getSetNumber();
            this.weight = set.getWeight();
            this.reps = set.getReps();
            this.restTime = set.getRestTime();
            this.memo = set.getMemo();
            this.completed = set.getCompleted();
            this.actualWeight = set.getActualWeight();
            this.actualReps = set.getActualReps();
            this.actualMemo = set.getActualMemo();
            this.completedAt = set.getCompletedAt();
        }
    }
}
