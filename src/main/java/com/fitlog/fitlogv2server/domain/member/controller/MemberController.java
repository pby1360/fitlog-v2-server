package com.fitlog.fitlogv2server.domain.member.controller;

import jakarta.validation.Valid;
import com.fitlog.fitlogv2server.domain.member.dto.MemberResponseDto;
import com.fitlog.fitlogv2server.domain.member.dto.MemberUpdateRequestDto;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.member.service.MemberWithdrawalService;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberWithdrawalService memberWithdrawalService;

    /**
     * 내 프로필 조회 API
     * GET /api/members/me
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberResponseDto response = memberService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 내 프로필 수정 API
     * PATCH /api/members/me
     */
    @PatchMapping("/me")
    public ResponseEntity<MemberResponseDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemberUpdateRequestDto requestDto) {
        MemberResponseDto response = memberService.updateProfile(userDetails.getId(), requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 탈퇴 API: 개인정보·운동 기록·로그인 세션을 모두 파기한다
     * DELETE /api/members/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        memberWithdrawalService.withdraw(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
