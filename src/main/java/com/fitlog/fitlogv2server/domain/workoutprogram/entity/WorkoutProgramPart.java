package com.fitlog.fitlogv2server.domain.workoutprogram.entity;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
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
public class WorkoutProgramPart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_program_id")
    private WorkoutProgram workoutProgram;

    /**
     * [핵심] 마스터 데이터 참조
     * - 이름(String)을 저장하는 게 아니라, ID를 저장하여 관리합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_part_id", nullable = false)
    private WorkoutPart workoutPart;

    @Column(name = "orders") // order는 DB 예약어일 가능성이 높음
    private int order; // 순서 (1, 2, 3...)

    // 하위: 운동 종목 목록
    @OneToMany(mappedBy = "workoutProgramPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutProgramExercise> exercises = new ArrayList<>();

    @Builder
    public WorkoutProgramPart(WorkoutProgram workoutProgram, WorkoutPart workoutPart, int order) {
        this.workoutProgram = workoutProgram;
        this.workoutPart = workoutPart;
        this.order = order;
    }
}