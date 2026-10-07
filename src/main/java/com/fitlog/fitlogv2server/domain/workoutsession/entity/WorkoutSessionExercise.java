package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id")
    private WorkoutSession workoutSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    // 세션 생성 시점의 운동/부위 이름 (이후 종목 이름·부위가 바뀌거나 보관돼도 기록 표기는 유지)
    private String workoutName;
    private String bodyPartName;

    @Column(name = "`order`")
    private Integer order;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean skipped = false;

    @Column
    private ZonedDateTime startedAt;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workoutSessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkoutSessionSet> workoutSessionSets = new HashSet<>();

    @Builder
    public WorkoutSessionExercise(WorkoutSession workoutSession, Workout workout, Integer order) {
        this.workoutSession = workoutSession;
        this.workout = workout;
        if (workout != null) {
            this.workoutName = workout.getName();
            this.bodyPartName = workout.getWorkoutPart() != null ? workout.getWorkoutPart().getName() : null;
        }
        this.order = order;
        this.skipped = false;
    }

    // 스냅샷이 없는 과거 데이터는 현재 종목/부위 이름으로 대체한다
    public String getDisplayWorkoutName() {
        return this.workoutName != null ? this.workoutName : this.workout.getName();
    }

    public String getDisplayBodyPartName() {
        return this.bodyPartName != null ? this.bodyPartName : this.workout.getWorkoutPart().getName();
    }

    public void addWorkoutSessionSet(WorkoutSessionSet workoutSessionSet) {
        this.workoutSessionSets.add(workoutSessionSet);
    }

    public void skip() {
        this.skipped = true;
    }

    public void unskip() {
        this.skipped = false;
    }

    public void updateOrder(int order) {
        this.order = order;
    }

    public void updateStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
