package com.fitlog.fitlogv2server.global.security.token;

import com.fitlog.fitlogv2server.domain.member.entity.Role;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final TokenProvider tokenProvider = new TokenProvider(TokenProviderTest.SECRET, 3600, 604800);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessToken_authenticatesRequest() throws Exception {
        String accessToken = tokenProvider.createAccessToken(5L, "a@example.com", "a", Role.USER);

        Authentication authentication = runFilter(accessToken);

        assertThat(authentication).isNotNull();
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getId()).isEqualTo(5L);
    }

    @Test
    void refreshToken_doesNotAuthenticateRequest() throws Exception {
        String refreshToken = tokenProvider.createRefreshToken(5L);

        assertThat(runFilter(refreshToken)).isNull();
    }

    private Authentication runFilter(String bearerToken) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + bearerToken);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
