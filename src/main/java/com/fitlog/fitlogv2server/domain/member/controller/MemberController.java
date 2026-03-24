package com.fitlog.fitlogv2server.domain.member.controller;

import com.fitlog.fitlogv2server.domain.member.dto.MemberResponseDto;
import com.fitlog.fitlogv2server.domain.member.dto.MemberUpdateRequestDto;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
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
            @RequestBody MemberUpdateRequestDto requestDto) {
        MemberResponseDto response = memberService.updateProfile(userDetails.getId(), requestDto);
        return ResponseEntity.ok(response);
    }
}
