package com.fitlog.fitlogv2server.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequestDto {
    private String nickname;
    private String phone;
    private String birthDate;
    private Integer height;
    private Integer weight;
    private String goal;
    private String experience;

    // 신체정보는 "필드 미전송(변경 없음)"과 "명시적 null(값 삭제)"을 구분한다
    @JsonIgnore
    private boolean heightPresent;
    @JsonIgnore
    private boolean weightPresent;

    public void setHeight(Integer height) {
        this.height = height;
        this.heightPresent = true;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
        this.weightPresent = true;
    }
}
