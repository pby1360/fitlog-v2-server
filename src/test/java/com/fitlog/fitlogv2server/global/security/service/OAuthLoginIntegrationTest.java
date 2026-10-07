package com.fitlog.fitlogv2server.global.security.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Provider;
import com.fitlog.fitlogv2server.global.security.dto.OAuthAttributes;
import com.fitlog.fitlogv2server.global.security.oauth.JdbcOAuth2AuthorizationRequestRepository;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthLoginIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    private JdbcOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Test
    void memberIsIdentifiedByProviderId_notEmail() {
        String sub = UUID.randomUUID().toString();
        Member first = customOAuth2UserService.saveOrUpdate(google(sub, sub + "@gmail.com", "홍길동"));

        // 공급자 쪽 이메일이 바뀌어도 같은 계정으로 로그인된다
        Member again = customOAuth2UserService.saveOrUpdate(google(sub, "changed-" + sub + "@gmail.com", "홍길동"));

        assertThat(again.getId()).isEqualTo(first.getId());
    }

    @Test
    void sameEmailOnOtherProvider_isRejectedNotMerged() {
        String email = UUID.randomUUID() + "@example.com";
        customOAuth2UserService.saveOrUpdate(google(UUID.randomUUID().toString(), email, "구글사용자"));

        assertThatThrownBy(() -> customOAuth2UserService.saveOrUpdate(kakao(123456789L + System.nanoTime() % 1000, email)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo(CustomOAuth2UserService.ERROR_EMAIL_REGISTERED_WITH_OTHER_PROVIDER));
    }

    @Test
    void kakaoWithoutEmailOrNickname_stillSignsUp() {
        long kakaoId = System.nanoTime();
        OAuthAttributes attributes = OAuthAttributes.of("kakao", "id", Map.of("id", kakaoId, "kakao_account", Map.of()));

        Member member = customOAuth2UserService.saveOrUpdate(attributes);

        assertThat(member.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(member.getProviderId()).isEqualTo(String.valueOf(kakaoId));
        assertThat(member.getNickname()).isNotBlank();
    }

    @Test
    void authorizationRequest_isStoredInDbAndConsumedOnce() {
        String state = UUID.randomUUID().toString();
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-google-client")
                .redirectUri("http://localhost/login/oauth2/code/google")
                .state(state)
                .attributes(Map.of("registration_id", "google"))
                .build();
        authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest,
                new MockHttpServletRequest(), new MockHttpServletResponse());

        // 다른 인스턴스로 온 콜백이라고 가정: 메모리 세션 없이 state 만으로 조회된다
        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setParameter("state", state);
        assertThat(authorizationRequestRepository.loadAuthorizationRequest(callback)).isNotNull();

        OAuth2AuthorizationRequest removed = authorizationRequestRepository.removeAuthorizationRequest(callback, new MockHttpServletResponse());
        assertThat(removed.getState()).isEqualTo(state);
        assertThat(authorizationRequestRepository.removeAuthorizationRequest(callback, new MockHttpServletResponse())).isNull();
    }

    private OAuthAttributes google(String sub, String email, String name) {
        return OAuthAttributes.of("google", "sub", Map.of("sub", sub, "email", email, "name", name));
    }

    private OAuthAttributes kakao(long id, String email) {
        return OAuthAttributes.of("kakao", "id", Map.of("id", id,
                "kakao_account", Map.of("email", email, "profile", Map.of("nickname", "카카오사용자"))));
    }
}
