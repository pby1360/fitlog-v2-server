package com.fitlog.fitlogv2server.global.security.token; // ⬅️ 패키지 경로 확인

import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    // 실제 필터링 로직은 doFilterInternal에 구현
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String jwt = resolveToken(request);

        // 2. Access Token인지 검사 (Refresh Token·용도 없는 이전 토큰은 일반 API 인증에 사용할 수 없다)
        //    (StringUtils.hasText(jwt)는 jwt가 null이거나 ""이 아닌지 확인)
        Optional<Claims> claims = StringUtils.hasText(jwt) ? tokenProvider.parseAccessToken(jwt) : Optional.empty();
        if (claims.isPresent()) {
            // 3. 토큰이 유효할 경우 토큰에서 Authentication 객체 가져오기
            Authentication authentication = getAuthentication(claims.get());
            // 4. SecurityContext에 Authentication 객체 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), request.getRequestURI());
        } else {
            log.debug("유효한 JWT 토큰이 없습니다, uri: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response); // 다음 필터로 계속 진행
    }

    // Request Header에서 토큰 정보 꺼내오기
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7); // "Bearer " 7글자 제외
        }
        return null;
    }

    // 토큰에서 인증(Authentication) 객체 생성
    private Authentication getAuthentication(Claims claims) {
        if (claims.get("role") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다."); // [!] 추후 커스텀 예외로
        }

        // 클레임에서 정보 가져오기
        Long memberId = Long.valueOf(claims.getSubject()); // subject에 memberId 저장
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        String role = claims.get("role", String.class);

        // CustomUserDetails 객체 만들어서 Authentication 리턴
        CustomUserDetails principal = new CustomUserDetails(memberId, email, nickname, role);

        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }
}