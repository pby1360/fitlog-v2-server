package com.fitlog.fitlogv2server.domain.workoutsession.dto;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fitlog.fitlogv2server.global.common.ValidationLimits;
import lombok.Getter;

import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class WorkoutSessionDto {

    @Getter
    public static class StartRequest {
        @NotNull
        private Long workoutProgramId;
        @Size(max = ValidationLimits.MAX_EXERCISES)
        private List<@Valid CustomExerciseRequest> customExercises;
    }

    @Getter
    public static class CustomExerciseRequest {
        @NotNull
        private Long workoutId;
        @NotNull @Min(1)
        private Integer order;
        @NotNull @Size(max = ValidationLimits.MAX_SETS)
        private List<@Valid CustomSetRequest> sets;
    }

    @Getter
    public static class CustomSetRequest {
        private Integer setNumber; // 무시됨: 서버가 1부터 순서대로 부여
        @DecimalMin("0") @DecimalMax(ValidationLimits.MAX_WEIGHT)
        private Double weight;
        @NotNull @Min(0) @Max(ValidationLimits.MAX_REPS)
        private Integer reps;
        @NotNull @Min(0) @Max(ValidationLimits.MAX_REST_SECONDS)
        private Integer restTime;
        @Size(max = ValidationLimits.MAX_MEMO)
        private String memo;
    }

    @Getter
    public static class CompleteSetRequest {
        @NotNull
        private Long workoutSessionExerciseId;
        @NotNull
        private Long workoutSessionSetId;
        @DecimalMin("0") @DecimalMax(ValidationLimits.MAX_WEIGHT)
        private Double actualWeight;
        @Min(0) @Max(ValidationLimits.MAX_REPS)
        private Integer actualReps;
        @Size(max = ValidationLimits.MAX_MEMO)
        private String memo;
    }

    @Getter
    public static class EndRequest {
        private ZonedDateTime endTime; // 무시됨: 종료 시각은 서버가 기록
        @NotNull
        private SessionStatus status;
    }

    @Getter
    public static class SkipExerciseRequest {
        @NotNull
        private Long workoutSessionExerciseId;
        @NotNull
        private Boolean skipped;
    }

    @Getter
    public static class ReorderExercisesRequest {
        @NotEmpty @Size(max = ValidationLimits.MAX_EXERCISES)
        private List<@Valid ExerciseOrderItem> exercises;
    }

    @Getter
    public static class ExerciseOrderItem {
        @NotNull
        private Long workoutSessionExerciseId;
        @NotNull @Min(1)
        private Integer order;
    }

    @Getter
    public static class AddExerciseRequest {
        @NotNull
        private Long workoutId;
        @Min(1)
        private Integer order;
        @NotEmpty @Size(max = ValidationLimits.MAX_SETS)
        private List<@Valid AddSetRequest> sets;
    }

    @Getter
    public static class AddSetRequest {
        private Integer setNumber; // 무시됨: 서버가 1부터 순서대로 부여
        @DecimalMin("0") @DecimalMax(ValidationLimits.MAX_WEIGHT)
        private Double weight;
        @NotNull @Min(0) @Max(ValidationLimits.MAX_REPS)
        private Integer reps;
        @NotNull @Min(0) @Max(ValidationLimits.MAX_REST_SECONDS)
        private Integer restTime;
        @Size(max = ValidationLimits.MAX_MEMO)
        private String memo;
    }

    @Getter
    public static class RemoveExerciseRequest {
        private Long workoutSessionExerciseId;
    }

    @Getter
    public static class StartExerciseRequest {
        @NotNull
        private ZonedDateTime startedAt;
    }

    @Getter
    public static class CreateSetRequest {
        @DecimalMin("0") @DecimalMax(ValidationLimits.MAX_WEIGHT)
        private Double weight;
        @NotNull @Min(1) @Max(ValidationLimits.MAX_REPS)
        private Integer reps;
        @NotNull @Min(0) @Max(ValidationLimits.MAX_REST_SECONDS)
        private Integer restTime;
        @Size(max = ValidationLimits.MAX_MEMO)
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
        private Long durationSeconds; // 종료된 세션의 실제 운동 시간(초, 일시정지 제외). 진행 중이면 null
        private List<ExerciseResponse> exercises;

        public Response(WorkoutSession workoutSession) {
            this.id = workoutSession.getId();
            this.workoutProgramId = workoutSession.getWorkoutProgram().getId();
            this.workoutProgramName = workoutSession.getDisplayProgramName();
            this.startTime = workoutSession.getStartTime();
            this.endTime = workoutSession.getEndTime();
            this.status = workoutSession.getStatus().name();
            this.totalPausedSeconds = workoutSession.getTotalPausedSeconds();
            this.lastPausedAt = workoutSession.getLastPausedAt();
            this.durationSeconds = workoutSession.getDurationSeconds();
            this.exercises = workoutSession.getWorkoutSessionExercises().stream()
                    .sorted(Comparator.comparing(WorkoutSessionExercise::getOrder, Comparator.nullsLast(Comparator.naturalOrder())))
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
            this.workoutName = exercise.getDisplayWorkoutName();
            this.bodyPart = exercise.getDisplayBodyPartName();
            this.order = exercise.getOrder();
            this.skipped = exercise.getSkipped();
            this.startedAt = exercise.getStartedAt();
            this.sets = exercise.getWorkoutSessionSets().stream()
                    .sorted(Comparator.comparing(WorkoutSessionSet::getSetNumber, Comparator.nullsLast(Comparator.naturalOrder())))
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
            this.workoutProgramName = workoutSession.getDisplayProgramName();
            this.startTime = workoutSession.getStartTime();
            this.endTime = workoutSession.getEndTime();
            this.durationSeconds = workoutSession.getDurationSeconds();
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
                    .map(WorkoutSessionExercise::getDisplayBodyPartName)
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
            this.setNumber = set.getSetNumber() != null ? set.getSetNumber() : 0;
            this.weight = set.getWeight();
            this.reps = set.getReps() != null ? set.getReps() : 0;
            this.restTime = set.getRestTime() != null ? set.getRestTime() : 0;
            this.memo = set.getMemo();
            this.completed = Boolean.TRUE.equals(set.getCompleted());
            this.actualWeight = set.getActualWeight();
            this.actualReps = set.getActualReps();
            this.actualMemo = set.getActualMemo();
            this.completedAt = set.getCompletedAt();
        }
    }
}
