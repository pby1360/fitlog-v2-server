package com.fitlog.fitlogv2server.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
    private String nickname;
    private String phone;
    private String birthDate;
    private Integer height;
    private Integer weight;
    private String goal;
    private String experience;
}
