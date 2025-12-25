package com.fitlog.fitlogv2server.domain.workoutprogram.entity;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.global.common.BaseTimeEntity;
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
public class WorkoutProgram extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String name; // 예: "3분할 벌크업 루틴"

    private String description; // 예: "월/수/금 진행"

    // 하위: 파트 목록 (Day 1, Day 2...)
    @OneToMany(mappedBy = "workoutProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutProgramPart> parts = new ArrayList<>();

    @Builder
    public WorkoutProgram(Member member, String name, String description) {
        this.member = member;
        this.name = name;
        this.description = description;
    }
}