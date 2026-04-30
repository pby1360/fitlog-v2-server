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
        this.order = order;
        this.skipped = false;
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
