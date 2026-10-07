package com.fitlog.fitlogv2server.global.security.service;

import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.global.security.dto.OAuthAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    // 실패 핸들러가 사용자 안내 메시지를 고르는 데 쓰는 오류 코드
    public static final String ERROR_EMAIL_REGISTERED_WITH_OTHER_PROVIDER = "email_registered_with_other_provider";
    public static final String ERROR_MISSING_PROVIDER_ID = "missing_provider_id";

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 1. 현재 로그인 진행 중인 서비스(google/kakao) 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // 2. OAuth2 로그인 진행 시 키가 되는 필드값 (PK, Google: "sub", Kakao: "id")
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 3. OAuth2UserService를 통해 가져온 OAuth2User의 attribute를 DTO로 변환
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 4. DB에 사용자 저장 (가입 또는 업데이트)
        Member member = saveOrUpdate(attributes);

        // 5. Spring Security가 관리할 OAuth2User 객체 생성
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().getKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    // [핵심 로직] (공급자, 공급자 계정 ID)로 회원을 찾고, 없으면 가입시킨다
    Member saveOrUpdate(OAuthAttributes attributes) { // 테스트에서 호출하기 위해 package-private
        if (attributes.getProviderId() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(ERROR_MISSING_PROVIDER_ID));
        }

        Member member = memberRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                // 기존 회원: 이미지만 업데이트
                // 닉네임은 최초 가입 시에만 provider 이름으로 설정하고, 이후에는 사용자가 수정한 값을 보존한다
                .map(entity -> {
                    entity.updateImageUrl(attributes.getImageUrl());
                    return entity;
                })
                .orElseGet(() -> {
                    // 같은 이메일이 다른 공급자 계정으로 이미 가입돼 있으면 자동으로 합치지 않고 거부한다
                    memberRepository.findByEmail(attributes.getEmail()).ifPresent(existing -> {
                        throw new OAuth2AuthenticationException(new OAuth2Error(
                                ERROR_EMAIL_REGISTERED_WITH_OTHER_PROVIDER, existing.getProvider().name(), null));
                    });
                    return attributes.toEntity();
                });

        return memberRepository.save(member);
    }
}
