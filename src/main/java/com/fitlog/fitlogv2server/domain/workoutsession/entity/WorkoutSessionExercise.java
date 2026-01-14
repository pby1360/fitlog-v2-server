package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workoutSessionExercise", cascade = CascadeType.ALL)
    private Set<WorkoutSessionSet> workoutSessionSets = new HashSet<>();

    @Builder
    public WorkoutSessionExercise(WorkoutSession workoutSession, Workout workout, Integer order) {
        this.workoutSession = workoutSession;
        this.workout = workout;
        this.order = order;
    }

    public void addWorkoutSessionSet(WorkoutSessionSet workoutSessionSet) {
        this.workoutSessionSets.add(workoutSessionSet);
    }
}
