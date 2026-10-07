package com.fitlog.fitlogv2server.domain.auth.service;

import com.fitlog.fitlogv2server.domain.auth.repository.AuthSessionRepository;
import com.fitlog.fitlogv2server.domain.auth.repository.LoginCodeRepository;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.token.TokenHashes;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 토큰 발급·재발급·폐기와 로그인 교환 코드.
 * - 로그인(기기)마다 auth_session 1행. 여러 기기에서 동시에 로그인 상태를 유지할 수 있다.
 * - DB에는 Refresh 토큰/교환 코드의 SHA-256 해시만 저장한다.
 * - 재발급은 조건부 UPDATE 한 번으로 회전하므로 같은 토큰으로 동시에 요청해도 하나만 성공한다.
 * - 이미 회전된 토큰이 유예 시간 이후에 다시 쓰이면 탈취로 보고 그 세션을 폐기한다.
 */
@Slf4j
@Service
public class AuthService {

    // 로그인 교환 코드 유효 시간
    static final Duration LOGIN_CODE_TTL = Duration.ofSeconds(60);
    // 여러 탭이 거의 동시에 같은 토큰으로 재발급할 때는 탈취로 보지 않는다
    static final Duration REUSE_GRACE = Duration.ofSeconds(60);

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final AuthSessionRepository authSessionRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final Duration refreshTokenTtl;

    public AuthService(TokenProvider tokenProvider,
                       MemberRepository memberRepository,
                       AuthSessionRepository authSessionRepository,
                       LoginCodeRepository loginCodeRepository,
                       @Value("${jwt.refresh-token-expiry}") long refreshTokenExpirySeconds) {
        this.tokenProvider = tokenProvider;
        this.memberRepository = memberRepository;
        this.authSessionRepository = authSessionRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.refreshTokenTtl = Duration.ofSeconds(refreshTokenExpirySeconds);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public record LoginResult(String accessToken, String refreshToken, String imageUrl, String provider) {
    }

    /**
     * OAuth 로그인 성공 직후 호출. URL에는 토큰 대신 이 일회용 코드만 싣는다.
     */
    @Transactional
    public String createLoginCode(Member member) {
        String code = TokenHashes.randomUrlSafe();
        loginCodeRepository.save(TokenHashes.sha256Hex(code), member.getId(), Instant.now().plus(LOGIN_CODE_TTL));
        return code;
    }

    /**
     * 교환 코드를 토큰으로 바꾼다. 코드는 한 번만 사용할 수 있고 60초 후 만료된다.
     */
    @Transactional
    public LoginResult exchangeLoginCode(String code) {
        if (code == null || code.isBlank()) {
            throw unauthorized("Invalid login code");
        }
        Long memberId = loginCodeRepository.consume(TokenHashes.sha256Hex(code))
                .orElseThrow(() -> unauthorized("Invalid or expired login code"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> unauthorized("Invalid or expired login code"));

        TokenPair tokens = issueTokens(member);
        return new LoginResult(tokens.accessToken(), tokens.refreshToken(), member.getImageUrl(), member.getProvider().name());
    }

    // 새 로그인 세션을 만든다
    @Transactional
    public TokenPair issueTokens(Member member) {
        authSessionRepository.deleteInactiveByMember(member.getId());

        String refreshToken = tokenProvider.createRefreshToken(member.getId());
        authSessionRepository.create(member.getId(), TokenHashes.sha256Hex(refreshToken), Instant.now().plus(refreshTokenTtl));

        return new TokenPair(createAccessToken(member), refreshToken);
    }

    // 재사용 감지로 세션을 폐기한 뒤 401을 던지므로, 그 폐기가 롤백되지 않도록 한다
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenPair reissue(String refreshToken) {
        Claims claims = Optional.ofNullable(refreshToken)
                .filter(token -> !token.isBlank())
                .flatMap(tokenProvider::parseRefreshToken)
                .orElseThrow(() -> unauthorized("Invalid refresh token"));
        long memberId = Long.parseLong(claims.getSubject());
        String currentHash = TokenHashes.sha256Hex(refreshToken);

        String newRefreshToken = tokenProvider.createRefreshToken(memberId);
        Optional<Long> rotated = authSessionRepository.rotate(
                currentHash, TokenHashes.sha256Hex(newRefreshToken), memberId, Instant.now().plus(refreshTokenTtl));

        if (rotated.isEmpty()) {
            detectReuse(currentHash, memberId);
            throw unauthorized("Invalid refresh token");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> unauthorized("Invalid refresh token"));
        return new TokenPair(createAccessToken(member), newRefreshToken);
    }

    /**
     * 서버 측 로그아웃: 이 기기의 로그인 세션만 폐기한다.
     * 이미 폐기됐거나 유효하지 않은 토큰이어도 결과는 동일하므로 예외를 던지지 않는다.
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || tokenProvider.parseRefreshToken(refreshToken).isEmpty()) {
            return;
        }
        authSessionRepository.revokeByHash(TokenHashes.sha256Hex(refreshToken));
    }

    // 회원 탈퇴 등: 모든 기기의 로그인 세션과 미사용 교환 코드를 삭제한다
    @Transactional
    public void revokeAllSessions(long memberId) {
        authSessionRepository.deleteAllByMember(memberId);
        loginCodeRepository.deleteAllByMember(memberId);
    }

    private void detectReuse(String presentedHash, long memberId) {
        authSessionRepository.findActiveByPreviousHash(presentedHash).ifPresent(session -> {
            boolean withinGrace = session.rotatedAt().plus(REUSE_GRACE).isAfter(Instant.now());
            if (!withinGrace) {
                log.warn("Refresh token reuse detected. Revoking auth session {} of member {}", session.sessionId(), memberId);
                authSessionRepository.revokeById(session.sessionId());
            }
        });
    }

    private String createAccessToken(Member member) {
        return tokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getNickname(), member.getRole());
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
