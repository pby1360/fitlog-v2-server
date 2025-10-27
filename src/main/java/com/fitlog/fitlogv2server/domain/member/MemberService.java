package com.fitlog.fitlogv2server.domain.member;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final이 붙은 필드의 생성자를 자동으로 생성
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * (로그인 구현 후) 내 정보 조회 시 사용할 메서드
     *
     * @param memberId (SecurityContext에서 가져온 사용자 ID)
     * @return Member
     */
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 회원을 찾을 수 없습니다.")); // [!] 추후 커스텀 예외로 변경
    }

    /**
     * (로그인 구현 후) 닉네임 변경 시 사용할 메서드
     *
     * @param memberId    (SecurityContext에서 가져온 사용자 ID)
     * @param newNickname (변경할 새 닉네임)
     */
    @Transactional // 쓰기 작업이므로 readOnly = false (기본값)
    public void updateNickname(Long memberId, String newNickname) {
        Member member = findMemberById(memberId);
        member.updateNickname(newNickname);
        // @Transactional에 의해 'Dirty Checking'이 일어나므로 .save() 호출 불필요
    }
}