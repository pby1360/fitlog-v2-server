package com.fitlog.fitlogv2server.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequestDto {
    // null 이면 변경하지 않는다. 값이 오면 공백만으로 이루어질 수 없다.
    @Size(min = 1, max = 30)
    @Pattern(regexp = ".*\\S.*", message = "공백만으로 된 닉네임은 사용할 수 없습니다.")
    private String nickname;
    @Min(50) @Max(300)
    private Integer height; // cm
    @Min(20) @Max(500)
    private Integer weight; // kg
    @Size(max = 50)
    private String goal;
    @Size(max = 50)
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
