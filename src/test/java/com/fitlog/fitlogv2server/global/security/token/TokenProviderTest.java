package com.fitlog.fitlogv2server.global.security.token;

import com.fitlog.fitlogv2server.domain.member.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    static final byte[] KEY_BYTES = new byte[64];
    static {
        Arrays.fill(KEY_BYTES, (byte) 7);
    }
    static final String SECRET = Base64.getEncoder().encodeToString(KEY_BYTES);

    private final TokenProvider tokenProvider = new TokenProvider(SECRET, 3600, 604800);

    @Test
    void accessToken_isAcceptedOnlyAsAccessToken() {
        String accessToken = tokenProvider.createAccessToken(1L, "a@example.com", "a", Role.USER);

        assertThat(tokenProvider.parseAccessToken(accessToken)).isPresent();
        assertThat(tokenProvider.parseRefreshToken(accessToken)).isEmpty();
    }

    @Test
    void refreshToken_isAcceptedOnlyAsRefreshToken() {
        String refreshToken = tokenProvider.createRefreshToken(1L);

        assertThat(tokenProvider.parseRefreshToken(refreshToken)).isPresent();
        assertThat(tokenProvider.parseAccessToken(refreshToken)).isEmpty();
        assertThat(tokenProvider.parseRefreshToken(refreshToken).get().getSubject()).isEqualTo("1");
    }

    @Test
    void refreshTokens_areUniqueEvenWhenIssuedAtSameTime() {
        assertThat(tokenProvider.createRefreshToken(1L)).isNotEqualTo(tokenProvider.createRefreshToken(1L));
    }

    @Test
    void legacyTokenWithoutTokenUse_isRejectedForBothUses() {
        // 용도 클레임이 없던 이전 버전 토큰 (같은 키로 서명)
        String legacy = Jwts.builder()
                .setSubject("1")
                .claim("email", "a@example.com")
                .claim("role", Role.USER.getKey())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY_BYTES), SignatureAlgorithm.HS512)
                .compact();

        assertThat(tokenProvider.parseAccessToken(legacy)).isEmpty();
        assertThat(tokenProvider.parseRefreshToken(legacy)).isEmpty();
    }

    @Test
    void expiredOrMalformedToken_isRejected() {
        TokenProvider expiredProvider = new TokenProvider(SECRET, -1, -1);
        String expired = expiredProvider.createAccessToken(1L, "a@example.com", "a", Role.USER);

        assertThat(tokenProvider.parseAccessToken(expired)).isEmpty();
        assertThat(tokenProvider.parseAccessToken("not-a-jwt")).isEmpty();
    }
}
