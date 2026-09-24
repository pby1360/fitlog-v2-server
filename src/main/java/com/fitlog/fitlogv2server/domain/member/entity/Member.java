package com.fitlog.fitlogv2server.domain.member.entity;

import com.fitlog.fitlogv2server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {
        // OAuth 계정 식별자 (V4)
        @UniqueConstraint(name = "uk_member_provider_provider_id", columnNames = {"provider", "provider_id"})
})
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    private Integer height;

    private Integer weight;

    private String goal;

    private String experience;

    @Builder
    public Member(String email, String nickname, String imageUrl, Role role, Provider provider, String providerId,
                  Integer height, Integer weight, String goal, String experience) {
        this.email = email;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.height = height;
        this.weight = weight;
        this.goal = goal;
        this.experience = experience;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateProfile(String nickname, String goal, String experience) {
        if (nickname != null) this.nickname = nickname;
        if (goal != null) this.goal = goal;
        if (experience != null) this.experience = experience;
    }

    // null이면 값을 삭제한다
    public void updateHeight(Integer height) {
        this.height = height;
    }

    // null이면 값을 삭제한다
    public void updateWeight(Integer weight) {
        this.weight = weight;
    }
}
