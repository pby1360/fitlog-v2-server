package com.fitlog.fitlogv2server.domain.member.dto;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String provider;

    public MemberResponseDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getNickname();
        this.provider = member.getProvider().name();
    }
}
