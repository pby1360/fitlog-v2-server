package com.fitlog.fitlogv2server.global.config;

import com.fitlog.fitlogv2server.global.security.handler.OAuth2LoginSuccessHandler;
import com.fitlog.fitlogv2server.global.security.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // [추가]
public class SecurityConfig {

    // [추가] 의존성 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // [1] 기본 설정 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // [2] (중요) 세션 정책: STATELESS (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // [3] (수정) API 경로별 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // /api/members/me 같은 인증 필요한 API
                        .requestMatchers("/api/members/**", "/api/workout/**").authenticated()
                        // 그 외 /api/ (추후 인증 필요한 API 추가)
                        .requestMatchers("/api/**").authenticated()
                        // 그 외 모든 요청(로그인 페이지, 루트 등) 허용
                        .anyRequest().permitAll()
                )

                // [4] (신규) OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // (선택) 로그인 페이지 URL (우리는 프론트에서 버튼 클릭)
                        // .loginPage("/login")
                        // (핵심) 로그인 성공 후 처리할 핸들러 등록
                        .successHandler(oAuth2LoginSuccessHandler)
                        // (핵심) OAuth2 로그인 시 사용자 정보를 가져올 서비스 등록
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                );

        // [5] (신규) JWT 필터 추가 (다음 단계에서 구현)
        // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}