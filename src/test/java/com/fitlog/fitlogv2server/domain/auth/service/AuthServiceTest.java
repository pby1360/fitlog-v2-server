package com.fitlog.fitlogv2server.domain.auth.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Role;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private TokenProvider tokenProvider;
    private AuthService authService;
    private Member member;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[64];
        Arrays.fill(keyBytes, (byte) 3);
        tokenProvider = new TokenProvider(Base64.getEncoder().encodeToString(keyBytes), 3600, 604800);
        authService = new AuthService(tokenProvider, memberRepository);

        member = Member.builder().email("a@example.com").nickname("a").role(Role.USER).build();
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    void issueTokens_storesOnlyHashOfRefreshToken() {
        AuthService.TokenPair tokens = authService.issueTokens(member);

        assertThat(member.getRefreshToken()).isNotEqualTo(tokens.refreshToken());
        assertThat(member.getRefreshToken()).isEqualTo(AuthService.hash(tokens.refreshToken()));
        assertThat(tokenProvider.parseAccessToken(tokens.accessToken())).isPresent();
        verify(memberRepository).save(member);
    }

    @Test
    void reissue_rotatesRefreshToken() {
        AuthService.TokenPair first = authService.issueTokens(member);
        given(memberRepository.findByRefreshToken(AuthService.hash(first.refreshToken()))).willReturn(Optional.of(member));

        AuthService.TokenPair second = authService.reissue(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(member.getRefreshToken()).isEqualTo(AuthService.hash(second.refreshToken()));
    }

    @Test
    void reissue_rejectsAccessToken() {
        String accessToken = tokenProvider.createAccessToken(1L, "a@example.com", "a", Role.USER);

        assertThatThrownBy(() -> authService.reissue(accessToken))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(memberRepository, never()).findByRefreshToken(anyString());
    }

    @Test
    void reissue_rejectsRevokedOrRotatedRefreshToken() {
        String oldRefreshToken = tokenProvider.createRefreshToken(1L);
        given(memberRepository.findByRefreshToken(AuthService.hash(oldRefreshToken))).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(oldRefreshToken))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reissue_rejectsWhenSubjectDoesNotMatchStoredMember() {
        String otherMembersToken = tokenProvider.createRefreshToken(2L);
        given(memberRepository.findByRefreshToken(AuthService.hash(otherMembersToken))).willReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.reissue(otherMembersToken))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void logout_revokesStoredRefreshToken() {
        AuthService.TokenPair tokens = authService.issueTokens(member);
        given(memberRepository.findByRefreshToken(AuthService.hash(tokens.refreshToken()))).willReturn(Optional.of(member));

        authService.logout(tokens.refreshToken());

        assertThat(member.getRefreshToken()).isNull();
    }

    @Test
    void logout_ignoresMissingOrInvalidToken() {
        authService.logout(null);
        authService.logout("not-a-jwt");

        verify(memberRepository, never()).findByRefreshToken(anyString());
    }
}
