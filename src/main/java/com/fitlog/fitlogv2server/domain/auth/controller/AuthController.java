package com.fitlog.fitlogv2server.domain.auth.controller;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        Member member = memberRepository.findByRefreshToken(refreshToken)
                .orElse(null);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found");
        }

        String newAccessToken = tokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole());
        String newRefreshToken = tokenProvider.createRefreshToken(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole());

        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }
}
