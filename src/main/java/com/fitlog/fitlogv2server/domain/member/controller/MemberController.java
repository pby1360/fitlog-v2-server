package com.fitlog.fitlogv2server.domain.member.controller;

import com.fitlog.fitlogv2server.domain.member.dto.MemberResponseDto;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members") // 모든 /api/members 요청은 이 컨트롤러로
public class MemberController {

    private final MemberService memberService;

    /**
     * (로그인 구현 후) 내 정보 조회 API
     *
     * [참고]
     * 실제 구현 시: @AuthenticationPrincipal을 사용해 로그인된 사용자 ID를 가져와야 합니다.
     * public ResponseEntity<MemberResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
     * Long memberId = userDetails.getMemberId();
     * Member member = memberService.findMemberById(memberId);
     * return ResponseEntity.ok(new MemberResponseDto(member));
     * }
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = memberService.findMemberById(userDetails.getId());
        return ResponseEntity.ok(new MemberResponseDto(member));
    }

    /**
     * (로그인 구현 후) 닉네임 수정 API
     *
     * [참고]
     * 실제 구현 시: @RequestBody로 DTO를 받아 처리합니다.
     * public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
     * @RequestBody MemberNicknameUpdateRequestDto requestDto) {
     * Long memberId = userDetails.getMemberId();
     * memberService.updateNickname(memberId, requestDto.getNickname());
     * return ResponseEntity.ok().build();
     * }
     */
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname() {
        // [!] 로그인 기능 구현 전 임시
        // memberService.updateNickname(1L, "새닉네임");
        return ResponseEntity.ok().build();
    }
}