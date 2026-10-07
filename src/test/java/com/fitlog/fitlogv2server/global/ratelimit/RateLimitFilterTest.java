package com.fitlog.fitlogv2server.global.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-24T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimitFilter filter = new RateLimitFilter(
            new FixedWindowRateLimiter(2, Duration.ofMinutes(1), clock),
            new FixedWindowRateLimiter(3, Duration.ofMinutes(1), clock),
            new ObjectMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authEndpoint_isLimitedPerClientIp_andResetsAfterWindow() throws Exception {
        assertThat(call("POST", "/api/auth/refresh", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(call("POST", "/api/auth/refresh", "1.1.1.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse limited = call("POST", "/api/auth/refresh", "1.1.1.1");
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
        assertThat(limited.getContentAsString()).contains("TOO_MANY_REQUESTS");

        // 다른 IP는 영향 없음, 구간이 지나면 다시 허용
        assertThat(call("POST", "/api/auth/refresh", "2.2.2.2").getStatus()).isEqualTo(200);
        clock.advance(Duration.ofMinutes(1));
        assertThat(call("POST", "/api/auth/refresh", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void clientIp_usesLastForwardedEntry_soSpoofedPrefixDoesNotBypass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader("X-Forwarded-For", "9.9.9.9, 1.1.1.1");

        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("1.1.1.1");
    }

    @Test
    void writeApi_isLimitedPerMember_readsAreNotLimited() throws Exception {
        authenticate(7L);
        for (int i = 0; i < 3; i++) {
            assertThat(call("POST", "/api/workout-sessions", "1.1.1.1").getStatus()).isEqualTo(200);
        }
        assertThat(call("PATCH", "/api/workout-sessions/1/complete-set", "1.1.1.1").getStatus()).isEqualTo(429);
        assertThat(call("GET", "/api/workout-sessions/latest", "1.1.1.1").getStatus()).isEqualTo(200);

        authenticate(8L);
        assertThat(call("POST", "/api/workout-sessions", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    private void authenticate(long memberId) {
        CustomUserDetails user = new CustomUserDetails(memberId, "u@example.com", "u", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    private MockHttpServletResponse call(String method, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
