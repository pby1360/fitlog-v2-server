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
@Table(name = "member")
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

    private String phone;

    private String birthDate;

    private Integer height;

    private Integer weight;

    private String goal;

    private String experience;

    @Column(length = 512)
    private String refreshToken;

    @Builder
    public Member(String email, String nickname, String imageUrl, Role role, Provider provider, String providerId,
                  String phone, String birthDate, Integer height, Integer weight, String goal, String experience) {
        this.email = email;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.phone = phone;
        this.birthDate = birthDate;
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

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateProfile(String nickname, String phone, String birthDate,
                              Integer height, Integer weight, String goal, String experience) {
        if (nickname != null) this.nickname = nickname;
        if (phone != null) this.phone = phone;
        if (birthDate != null) this.birthDate = birthDate;
        if (height != null) this.height = height;
        if (weight != null) this.weight = weight;
        if (goal != null) this.goal = goal;
        if (experience != null) this.experience = experience;
    }
}
