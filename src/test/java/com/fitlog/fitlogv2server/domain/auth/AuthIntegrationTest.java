package com.fitlog.fitlogv2server.domain.auth;

import com.fitlog.fitlogv2server.domain.auth.service.AuthService;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Role;
import com.fitlog.fitlogv2server.global.security.token.TokenHashes;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AuthService authService;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginCode_canBeExchangedOnlyOnce() {
        Member member = fixtures.member();
        String code = authService.createLoginCode(member);

        AuthService.LoginResult result = authService.exchangeLoginCode(code);

        assertThat(tokenProvider.parseAccessToken(result.accessToken())).isPresent();
        assertThat(result.provider()).isEqualTo("GOOGLE");
        assertUnauthorized(() -> authService.exchangeLoginCode(code));
    }

    @Test
    void expiredLoginCode_isRejected() {
        Member member = fixtures.member();
        String code = authService.createLoginCode(member);
        jdbcTemplate.update("UPDATE auth_login_code SET expires_at = now() - interval '1 second' WHERE code_hash = ?",
                TokenHashes.sha256Hex(code));

        assertUnauthorized(() -> authService.exchangeLoginCode(code));
    }

    @Test
    void loginCode_storesOnlyHash() {
        Member member = fixtures.member();
        String code = authService.createLoginCode(member);

        Integer plain = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auth_login_code WHERE code_hash = ?", Integer.class, code);
        assertThat(plain).isZero();
    }

    @Test
    void reissue_rotatesAndRejectsOldToken() {
        Member member = fixtures.member();
        AuthService.TokenPair first = authService.issueTokens(member);

        AuthService.TokenPair second = authService.reissue(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertUnauthorized(() -> authService.reissue(first.refreshToken()));
        // 유예 시간 안의 재사용(여러 탭)은 세션을 폐기하지 않는다
        assertThat(authService.reissue(second.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void reuseOfRotatedTokenAfterGrace_revokesSession() {
        Member member = fixtures.member();
        AuthService.TokenPair first = authService.issueTokens(member);
        AuthService.TokenPair second = authService.reissue(first.refreshToken());
        jdbcTemplate.update("UPDATE auth_session SET rotated_at = now() - interval '10 minutes' WHERE member_id = ?", member.getId());

        // 탈취된 옛 토큰이 나중에 쓰이면 → 거부 + 세션 폐기 → 정상 사용자의 최신 토큰도 무효
        assertUnauthorized(() -> authService.reissue(first.refreshToken()));
        assertUnauthorized(() -> authService.reissue(second.refreshToken()));
    }

    @Test
    void multipleDevices_keepIndependentSessions() {
        Member member = fixtures.member();
        AuthService.TokenPair phone = authService.issueTokens(member);
        AuthService.TokenPair laptop = authService.issueTokens(member);

        authService.logout(phone.refreshToken());

        assertUnauthorized(() -> authService.reissue(phone.refreshToken()));
        assertThat(authService.reissue(laptop.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void accessToken_cannotBeUsedToReissue() {
        Member member = fixtures.member();
        String accessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getNickname(), Role.USER);

        assertUnauthorized(() -> authService.reissue(accessToken));
    }

    @Test
    void refreshTokenOfAnotherMember_isRejected() {
        Member victim = fixtures.member();
        Member attacker = fixtures.member();
        AuthService.TokenPair victimTokens = authService.issueTokens(victim);
        // 서명은 유효하지만 subject 가 다른 회원인 토큰은 세션과 매칭되지 않는다
        String forged = tokenProvider.createRefreshToken(attacker.getId());

        assertUnauthorized(() -> authService.reissue(forged));
        assertThat(authService.reissue(victimTokens.refreshToken()).accessToken()).isNotBlank();
    }

    private void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
