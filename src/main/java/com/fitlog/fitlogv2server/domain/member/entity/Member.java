package com.fitlog.fitlogv2server.domain.member.entity;

import com.fitlog.fitlogv2server.global.common.BaseTimeEntity; // 2번에서 만들 BaseTimeEntity
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자가 필요합니다.
@Table(name = "member")
public class Member extends BaseTimeEntity { // 2번에서 만들 BaseTimeEntity 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // 소셜 로그인 ID(이메일)로 사용

    @Column(nullable = false)
    private String nickname; // 앱에서 사용할 닉네임 (초기값은 소셜 프로필 이름)

    private String imageUrl; // 소셜 프로필 이미지 URL (선택 사항)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 사용자 권한 (ROLE_USER, ROLE_ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // 소셜 로그인 제공자 (GOOGLE, KAKAO)

    @Column(nullable = false)
    private String providerId; // 소셜 로그인 제공자의 고유 ID

    @Builder
    public Member(String email, String nickname, String imageUrl, Role role, Provider provider, String providerId) {
        this.email = email;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 닉네임 변경 등 프로필 업데이트를 위한 메서드
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 프로필 이미지 업데이트 메서드
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
