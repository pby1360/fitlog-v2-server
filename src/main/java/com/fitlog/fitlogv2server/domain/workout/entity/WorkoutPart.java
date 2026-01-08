package com.fitlog.fitlogv2server.domain.workout.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
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
public class WorkoutPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예: "가슴", "등", "하체", "어깨", "이두", "삼두", "복근"
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * [시스템/커스텀 구분]
     * null: 시스템 기본 운동
     * not null: 사용자 커스텀 운동
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    /**
     * [1 : N 관계 설정]
     * mappedBy = "workoutPart": Workout 엔티티의 workoutPart 필드가 주인임을 명시
     * CascadeType.ALL: 부위가 삭제되면 하위 운동도 같이 삭제되는 것이 보통이나,
     * 마스터 데이터이므로 삭제 로직은 신중해야 함 (보통은 ALL 안 씀, 상황에 맞게 조정)
     */
    @OneToMany(mappedBy = "workoutPart")
    private List<Workout> workouts = new ArrayList<>();

    @Builder
    public WorkoutPart(String name, Member member) {
        this.name = name;
        this.member = member;
    }

    public void updateName(String name) {
        this.name = name;
    }
}