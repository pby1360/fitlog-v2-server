package com.fitlog.fitlogv2server.global.security.handler;


import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.global.security.dto.OAuthAttributes;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    @Value("${app.client-url}")
    private String clientUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. 인증 객체에서 OAuth2User 추출
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 2. registrationId(google/kakao)와 attributes 추출
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. DTO로 변환 (email 등 정보 추출)
        OAuthAttributes attrDto = OAuthAttributes.of(registrationId, "id", attributes);
        String email = attrDto.getEmail();

        // 4. DB에서 사용자 정보 조회 (CustomOAuth2UserService에서 이미 저장/업데이트됨)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 인증 후 사용자를 찾을 수 없습니다."));

        // 5. Access Token, Refresh Token 생성
        String accessToken = tokenProvider.createAccessToken(member.getId(), email, member.getNickname(), member.getRole());
        String refreshToken = tokenProvider.createRefreshToken(member.getId(), email, member.getNickname(), member.getRole());

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);

        // 6. 프론트엔드로 리다이렉트 (토큰 및 provider 정보를 쿼리 파라미터로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(clientUrl + "/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("imageUrl", member.getImageUrl())
                .queryParam("provider", member.getProvider().name())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}