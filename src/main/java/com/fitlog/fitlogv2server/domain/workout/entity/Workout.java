package com.fitlog.fitlogv2server.domain.workout.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "workout_name_part_id_unique",
                        columnNames = {"name", "workout_part_id"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 예: "벤치프레스", "스쿼트"

    /**
     * [N : 1 관계 설정]
     * 연관관계의 주인 (FK를 가짐)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_part_id", nullable = false)
    private WorkoutPart workoutPart;

    /**
     * [시스템/커스텀 구분]
     * null: 시스템 기본 운동
     * not null: 사용자 커스텀 운동
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Builder
    public Workout(String name, WorkoutPart workoutPart, Member member) {
        this.name = name;
        this.member = member;
        setWorkoutPart(workoutPart); // 연관관계 편의 메서드 호출
    }

    // --- 연관관계 편의 메서드 ---
    // 객체 양방향 관계를 안전하게 맺어줍니다.
    public void setWorkoutPart(WorkoutPart workoutPart) {
        // 1. 기존 관계 제거 (혹시 있다면)
        if (this.workoutPart != null) {
            this.workoutPart.getWorkouts().remove(this);
        }

        // 2. 새로운 관계 설정
        this.workoutPart = workoutPart;

        // 3. 반대쪽 리스트에도 추가 (무한루프 방지 체크)
        if (workoutPart != null && !workoutPart.getWorkouts().contains(this)) {
            workoutPart.getWorkouts().add(this);
        }
    }
}