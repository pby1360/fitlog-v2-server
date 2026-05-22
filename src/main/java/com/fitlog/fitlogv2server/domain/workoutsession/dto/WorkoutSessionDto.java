package com.fitlog.fitlogv2server.domain.workoutsession.dto;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import lombok.Getter;

import org.springframework.data.domain.Page;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class WorkoutSessionDto {

    @Getter
    public static class StartRequest {
        private Long workoutProgramId;
        private List<CustomExerciseRequest> customExercises;
    }

    @Getter
    public static class CustomExerciseRequest {
        private Long workoutId;
        private Integer order;
        private List<CustomSetRequest> sets;
    }

    @Getter
    public static class CustomSetRequest {
        private Integer setNumber;
        private Double weight;
        private Integer reps;
        private Integer restTime;
        private String memo;
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
    public static class SkipExerciseRequest {
        private Long workoutSessionExerciseId;
        private Boolean skipped;
    }

    @Getter
    public static class ReorderExercisesRequest {
        private List<ExerciseOrderItem> exercises;
    }

    @Getter
    public static class ExerciseOrderItem {
        private Long workoutSessionExerciseId;
        private Integer order;
    }

    @Getter
    public static class AddExerciseRequest {
        private Long workoutId;
        private Integer order;
        private List<AddSetRequest> sets;
    }

    @Getter
    public static class AddSetRequest {
        private Integer setNumber;
        private Double weight;
        private Integer reps;
        private Integer restTime;
        private String memo;
    }

    @Getter
    public static class RemoveExerciseRequest {
        private Long workoutSessionExerciseId;
    }

    @Getter
    public static class StartExerciseRequest {
        private ZonedDateTime startedAt;
    }

    @Getter
    public static class CreateSetRequest {
        private Double weight;
        private Integer reps;
        private Integer restTime;
        private String memo;
    }

    @Getter
    public static class Response {
        private Long id;
        private Long workoutProgramId;
        private String workoutProgramName;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private String status;
        private Long totalPausedSeconds;
        private ZonedDateTime lastPausedAt;
        private List<ExerciseResponse> exercises;

        public Response(WorkoutSession workoutSession) {
            this.id = workoutSession.getId();
            this.workoutProgramId = workoutSession.getWorkoutProgram().getId();
            this.workoutProgramName = workoutSession.getWorkoutProgram().getName();
            this.startTime = workoutSession.getStartTime();
            this.endTime = workoutSession.getEndTime();
            this.status = workoutSession.getStatus().name();
            this.totalPausedSeconds = workoutSession.getTotalPausedSeconds();
            this.lastPausedAt = workoutSession.getLastPausedAt();
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
        private String bodyPart;
        private int order;
        private boolean skipped;
        private ZonedDateTime startedAt;
        private List<SetResponse> sets;

        public ExerciseResponse(WorkoutSessionExercise exercise) {
            this.id = exercise.getId();
            this.workoutId = exercise.getWorkout().getId();
            this.workoutName = exercise.getWorkout().getName();
            this.bodyPart = exercise.getWorkout().getWorkoutPart().getName();
            this.order = exercise.getOrder();
            this.skipped = exercise.getSkipped();
            this.startedAt = exercise.getStartedAt();
            this.sets = exercise.getWorkoutSessionSets().stream()
                    .sorted(Comparator.comparing(WorkoutSessionSet::getSetNumber))
                    .map(SetResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    public static class LogSummaryResponse {
        private Long id;
        private Long workoutProgramId;
        private String workoutProgramName;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private Long durationSeconds;
        private String status;
        private int totalExercises;
        private int completedExercises;
        private int totalSets;
        private int completedSets;
        private List<String> bodyParts;

        public LogSummaryResponse(WorkoutSession workoutSession) {
            this.id = workoutSession.getId();
            this.workoutProgramId = workoutSession.getWorkoutProgram().getId();
            this.workoutProgramName = workoutSession.getWorkoutProgram().getName();
            this.startTime = workoutSession.getStartTime();
            this.endTime = workoutSession.getEndTime();
            this.durationSeconds = (workoutSession.getStartTime() != null && workoutSession.getEndTime() != null)
                    ? Duration.between(workoutSession.getStartTime(), workoutSession.getEndTime()).getSeconds()
                            - (workoutSession.getTotalPausedSeconds() != null ? workoutSession.getTotalPausedSeconds() : 0L)
                    : null;
            this.status = workoutSession.getStatus().name();

            List<WorkoutSessionExercise> exercises = new java.util.ArrayList<>(workoutSession.getWorkoutSessionExercises());
            this.totalExercises = exercises.size();
            this.completedExercises = (int) exercises.stream()
                    .filter(e -> !e.getWorkoutSessionSets().isEmpty()
                            && e.getWorkoutSessionSets().stream().allMatch(s -> Boolean.TRUE.equals(s.getCompleted())))
                    .count();
            this.totalSets = exercises.stream().mapToInt(e -> e.getWorkoutSessionSets().size()).sum();
            this.completedSets = (int) exercises.stream()
                    .flatMap(e -> e.getWorkoutSessionSets().stream())
                    .filter(s -> Boolean.TRUE.equals(s.getCompleted()))
                    .count();
            this.bodyParts = exercises.stream()
                    .map(e -> e.getWorkout().getWorkoutPart().getName())
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    @Getter
    public static class LogPageResponse {
        private List<LogSummaryResponse> content;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private boolean first;
        private boolean last;
        private long totalDurationSeconds;
        private long totalCompletedSets;
        private long totalSets;
        private int averageCompletionRate;

        public LogPageResponse(Page<LogSummaryResponse> page, long totalDurationSeconds, long totalCompletedSets, long totalSets) {
            this.content = page.getContent();
            this.currentPage = page.getNumber();
            this.totalPages = page.getTotalPages();
            this.totalElements = page.getTotalElements();
            this.first = page.isFirst();
            this.last = page.isLast();
            this.totalDurationSeconds = totalDurationSeconds;
            this.totalCompletedSets = totalCompletedSets;
            this.totalSets = totalSets;
            this.averageCompletionRate = totalSets > 0
                    ? (int) Math.round((double) totalCompletedSets / totalSets * 100)
                    : 0;
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
        private ZonedDateTime completedAt;

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
