package com.fitlog.fitlogv2server.domain.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.domain.member.dto.MemberUpdateRequestDto;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Provider;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    @InjectMocks
    private MemberService memberService;

    private static final Long MEMBER_ID = 10L;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("user@example.com")
                .nickname("user")
                .provider(Provider.GOOGLE)
                .height(180)
                .weight(75)
                .build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
    }

    @Test
    void updateProfile_clearsBodyMeasurementsWhenExplicitNull() throws Exception {
        MemberUpdateRequestDto dto = objectMapper.readValue(
                "{\"nickname\":\"user\",\"height\":null,\"weight\":null}", MemberUpdateRequestDto.class);

        memberService.updateProfile(MEMBER_ID, dto);

        assertThat(member.getHeight()).isNull();
        assertThat(member.getWeight()).isNull();
    }

    @Test
    void updateProfile_keepsBodyMeasurementsWhenFieldsOmitted() throws Exception {
        MemberUpdateRequestDto dto = objectMapper.readValue(
                "{\"nickname\":\"새닉네임\"}", MemberUpdateRequestDto.class);

        memberService.updateProfile(MEMBER_ID, dto);

        assertThat(member.getNickname()).isEqualTo("새닉네임");
        assertThat(member.getHeight()).isEqualTo(180);
        assertThat(member.getWeight()).isEqualTo(75);
    }

    @Test
    void updateProfile_updatesBodyMeasurementsWhenValuesProvided() throws Exception {
        MemberUpdateRequestDto dto = objectMapper.readValue(
                "{\"height\":170,\"weight\":null}", MemberUpdateRequestDto.class);

        memberService.updateProfile(MEMBER_ID, dto);

        assertThat(member.getHeight()).isEqualTo(170);
        assertThat(member.getWeight()).isNull();
    }
}
