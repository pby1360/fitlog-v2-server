package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_program_id")
    private WorkoutProgram workoutProgram;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL)
    private Set<WorkoutSessionExercise> workoutSessionExercises = new HashSet<>();

    @Builder
    public WorkoutSession(Member member, WorkoutProgram workoutProgram, LocalDateTime startTime, LocalDateTime endTime, SessionStatus status) {
        this.member = member;
        this.workoutProgram = workoutProgram;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    
    public void addWorkoutSessionExercise(WorkoutSessionExercise workoutSessionExercise) {
        this.workoutSessionExercises.add(workoutSessionExercise);
    }
}
