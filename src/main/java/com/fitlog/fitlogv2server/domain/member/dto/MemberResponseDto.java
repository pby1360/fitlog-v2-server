package com.fitlog.fitlogv2server.domain.member.dto;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import lombok.Getter;

@Getter
public class MemberResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String imageUrl;
    private String provider;
    private Integer height;
    private Integer weight;
    private String goal;
    private String experience;
    private String createdAt;
    private long totalWorkoutDays;
    private long totalCompletedSets;
    private long totalDurationSeconds;

    public MemberResponseDto(Member member, long totalWorkoutDays, long totalCompletedSets, long totalDurationSeconds) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getNickname();
        this.imageUrl = member.getImageUrl();
        this.provider = member.getProvider().name();
        this.height = member.getHeight();
        this.weight = member.getWeight();
        this.goal = member.getGoal();
        this.experience = member.getExperience();
        this.createdAt = AppTimeZone.toKstDateString(member.getCreatedAt()); // 가입일(한국 날짜)
        this.totalWorkoutDays = totalWorkoutDays;
        this.totalCompletedSets = totalCompletedSets;
        this.totalDurationSeconds = totalDurationSeconds;
    }
}
