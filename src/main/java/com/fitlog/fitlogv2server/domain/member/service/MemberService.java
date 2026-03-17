package com.fitlog.fitlogv2server.domain.member.service;

import com.fitlog.fitlogv2server.domain.member.dto.MemberResponseDto;
import com.fitlog.fitlogv2server.domain.member.dto.MemberUpdateRequestDto;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final WorkoutSessionRepository workoutSessionRepository;

    /**
     * 내 정보 조회
     *
     * @param memberId SecurityContext에서 가져온 사용자 ID
     * @return Member
     */
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 회원을 찾을 수 없습니다."));
    }

    /**
     * 내 프로필 조회 (운동 통계 포함)
     *
     * @param memberId SecurityContext에서 가져온 사용자 ID
     * @return MemberResponseDto
     */
    public MemberResponseDto getMyProfile(Long memberId) {
        Member member = findMemberById(memberId);
        long totalWorkoutDays = workoutSessionRepository.countCompleted(memberId);
        long totalCompletedSets = workoutSessionRepository.sumCompletedSetsByMemberId(memberId);
        long totalDurationSeconds = workoutSessionRepository.sumDurationSecondsByMemberId(memberId);
        return new MemberResponseDto(member, totalWorkoutDays, totalCompletedSets, totalDurationSeconds);
    }

    /**
     * 프로필 업데이트 (닉네임, 전화번호, 생년월일, 신장, 체중, 목표, 경력)
     *
     * @param memberId   SecurityContext에서 가져온 사용자 ID
     * @param dto        업데이트 요청 DTO
     * @return MemberResponseDto
     */
    @Transactional
    public MemberResponseDto updateProfile(Long memberId, MemberUpdateRequestDto dto) {
        Member member = findMemberById(memberId);
        member.updateProfile(
                dto.getNickname(),
                dto.getPhone(),
                dto.getBirthDate(),
                dto.getHeight(),
                dto.getWeight(),
                dto.getGoal(),
                dto.getExperience()
        );
        long totalWorkoutDays = workoutSessionRepository.countCompleted(memberId);
        long totalCompletedSets = workoutSessionRepository.sumCompletedSetsByMemberId(memberId);
        long totalDurationSeconds = workoutSessionRepository.sumDurationSecondsByMemberId(memberId);
        return new MemberResponseDto(member, totalWorkoutDays, totalCompletedSets, totalDurationSeconds);
    }

    /**
     * 닉네임 변경 (하위 호환용)
     *
     * @param memberId    SecurityContext에서 가져온 사용자 ID
     * @param newNickname 변경할 새 닉네임
     */
    @Transactional
    public void updateNickname(Long memberId, String newNickname) {
        Member member = findMemberById(memberId);
        member.updateNickname(newNickname);
    }
}
