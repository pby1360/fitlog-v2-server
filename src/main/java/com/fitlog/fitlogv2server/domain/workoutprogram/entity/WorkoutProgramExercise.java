package com.fitlog.fitlogv2server.domain.workoutprogram.entity;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout; // 중요: 마스터 엔티티 import
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutProgramExercise {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_program_part_id")
    private WorkoutProgramPart workoutProgramPart;

    /**
     * [핵심] 마스터 데이터 참조
     * - 이름(String)을 저장하는 게 아니라, ID를 저장하여 관리합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(name = "orders")
    private int order; // 운동 순서 (1. 벤치 -> 2. 인클라인...)

    // 하위: 목표 세트 목록
    @OneToMany(mappedBy = "workoutProgramExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutProgramSet> sets = new ArrayList<>();

    @Builder
    public WorkoutProgramExercise(WorkoutProgramPart workoutProgramPart, Workout workout, int order) {
        this.workoutProgramPart = workoutProgramPart;
        this.workout = workout;
        this.order = order;
    }
}