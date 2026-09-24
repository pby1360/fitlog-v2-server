package com.fitlog.fitlogv2server.global.security.dto;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Provider;
import com.fitlog.fitlogv2server.domain.member.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private static final String DEFAULT_NICKNAME = "사용자";

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String imageUrl;
    private Provider provider;
    // 공급자 내에서 변하지 않는 계정 식별자 (Google: sub, Kakao: id). 이메일 대신 이 값으로 회원을 찾는다.
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email,
                           String imageUrl, Provider provider, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.imageUrl = imageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    // [1] registrationId(google/kakao)에 따라 올바른 ofXXX() 메서드 호출
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    // [2] 구글용 파싱
    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .imageUrl((String) attributes.get("picture"))
                .provider(Provider.GOOGLE)
                .providerId(stringOrNull(attributes.get("sub")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // [3] 카카오용 파싱 (데이터가 kakao_account, profile 등 중첩 구조이며 동의 항목에 따라 비어 있을 수 있음)
    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
        String kakaoId = stringOrNull(attributes.get("id"));

        // 이메일 동의 비활성화 시 null → Kakao ID 기반 대체 이메일 생성
        String email = (String) kakaoAccount.get("email");
        if (email == null) {
            email = "kakao_" + kakaoId + "@kakao.local";
        }

        return OAuthAttributes.builder()
                .name((String) kakaoProfile.get("nickname"))
                .email(email)
                .imageUrl((String) kakaoProfile.get("profile_image_url"))
                .provider(Provider.KAKAO)
                .providerId(kakaoId)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static String stringOrNull(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    // [4] OAuthAttributes -> Member Entity 변환
    // (이 시점은 '최초' 가입이므로 Role은 USER)
    public Member toEntity() {
        return Member.builder()
                .nickname(name != null && !name.isBlank() ? name : DEFAULT_NICKNAME) // 최초 가입 시에만 공급자 이름 사용
                .email(email)
                .imageUrl(imageUrl)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER) // 가입 시 기본 권한
                .build();
    }
}
