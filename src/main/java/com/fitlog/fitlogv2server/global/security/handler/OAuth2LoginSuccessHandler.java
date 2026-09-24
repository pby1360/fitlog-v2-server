package com.fitlog.fitlogv2server.global.security.handler;


import com.fitlog.fitlogv2server.domain.auth.service.AuthService;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.dto.OAuthAttributes;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final MemberRepository memberRepository;

    @Value("${app.client-url}")
    private String clientUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. 인증 객체에서 OAuth2User 와 공급자(google/kakao) 추출
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        // 2. (공급자, 공급자 계정 ID) 로 회원 조회 (CustomOAuth2UserService에서 이미 저장/업데이트됨)
        OAuthAttributes attrDto = OAuthAttributes.of(registrationId, null, oAuth2User.getAttributes());
        Member member = memberRepository.findByProviderAndProviderId(attrDto.getProvider(), attrDto.getProviderId())
                .orElseThrow(() -> new IllegalStateException("OAuth2 인증 후 사용자를 찾을 수 없습니다."));

        // 3. 토큰 대신 60초짜리 일회용 교환 코드만 URL로 전달한다.
        //    프론트는 이 코드로 POST /api/auth/token 을 호출해 토큰을 받는다 (URL·방문 기록에 토큰이 남지 않음).
        String code = authService.createLoginCode(member);
        String targetUrl = UriComponentsBuilder.fromUriString(clientUrl + "/auth/callback")
                .queryParam("code", code)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
