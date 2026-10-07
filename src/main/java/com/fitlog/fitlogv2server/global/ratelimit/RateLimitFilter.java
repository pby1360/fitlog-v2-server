package com.fitlog.fitlogv2server.global.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.global.exception.ErrorCode;
import com.fitlog.fitlogv2server.global.exception.ErrorResponseBody;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 과다 요청 제한 (#34).
 * - /api/auth/** (토큰 교환·재발급·로그아웃): 클라이언트 IP 기준
 * - 그 밖의 쓰기 API(POST/PUT/PATCH/DELETE): 로그인 회원 기준
 * 초과 시 429 + Retry-After.
 *
 * JWT 인증 필터 뒤에서 동작해야 회원을 알 수 있으므로 SecurityConfig 에서 직접 생성해 등록한다
 * (@Component 로 만들면 서블릿 필터로도 자동 등록돼 인증 전에 먼저 실행된다).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final FixedWindowRateLimiter authLimiter;
    private final FixedWindowRateLimiter writeLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(FixedWindowRateLimiter authLimiter, FixedWindowRateLimiter writeLimiter, ObjectMapper objectMapper) {
        this.authLimiter = authLimiter;
        this.writeLimiter = writeLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        FixedWindowRateLimiter.Decision decision = null;

        if (path.startsWith("/api/auth/")) {
            decision = authLimiter.tryAcquire("ip:" + clientIp(request));
        } else if (path.startsWith("/api/") && WRITE_METHODS.contains(request.getMethod())) {
            Long memberId = currentMemberId();
            if (memberId != null) {
                decision = writeLimiter.tryAcquire("member:" + memberId);
            }
        }

        if (decision != null && !decision.allowed()) {
            response.setStatus(429);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    ErrorResponseBody.of(ErrorCode.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user.getId();
        }
        return null;
    }

    // Cloud Run 앞단(Google Front End)은 실제 클라이언트 IP를 X-Forwarded-For 마지막에 덧붙인다.
    // 클라이언트가 임의로 넣은 앞쪽 값은 신뢰하지 않는다.
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
