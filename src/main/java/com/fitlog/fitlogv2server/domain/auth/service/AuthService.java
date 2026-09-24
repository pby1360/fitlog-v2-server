package com.fitlog.fitlogv2server.domain.auth.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 토큰 발급·재발급·폐기.
 * DB에는 Refresh Token 원문 대신 SHA-256 해시만 저장한다 (DB 조회 권한만으로 토큰을 재사용할 수 없도록).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    public record TokenPair(String accessToken, String refreshToken) {
    }

    @Transactional
    public TokenPair issueTokens(Member member) {
        String accessToken = tokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole());
        String refreshToken = tokenProvider.createRefreshToken(member.getId());

        member.updateRefreshToken(hash(refreshToken));
        memberRepository.save(member);

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public TokenPair reissue(String refreshToken) {
        Member member = findMemberByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return issueTokens(member);
    }

    /**
     * 서버 측 로그아웃: 저장된 Refresh Token을 폐기한다.
     * 이미 폐기됐거나 유효하지 않은 토큰이어도 결과는 동일하므로 예외를 던지지 않는다.
     */
    @Transactional
    public void logout(String refreshToken) {
        findMemberByRefreshToken(refreshToken).ifPresent(Member::revokeRefreshToken);
    }

    private Optional<Member> findMemberByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        Optional<Claims> claims = tokenProvider.parseRefreshToken(refreshToken);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        String memberId = claims.get().getSubject();
        return memberRepository.findByRefreshToken(hash(refreshToken))
                .filter(member -> String.valueOf(member.getId()).equals(memberId));
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
