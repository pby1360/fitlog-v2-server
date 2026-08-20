package com.fitlog.fitlogv2server.global.config;

import com.fitlog.fitlogv2server.global.security.handler.JwtAuthenticationEntryPoint;
import com.fitlog.fitlogv2server.global.security.handler.OAuth2AuthenticationFailureHandler;
import com.fitlog.fitlogv2server.global.security.handler.OAuth2LoginSuccessHandler;
import com.fitlog.fitlogv2server.global.security.service.CustomOAuth2UserService;
import com.fitlog.fitlogv2server.global.security.token.JwtAuthenticationFilter; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // [추가]
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * CORS 허용 오리진. 쉼표로 구분한다.
     * 환경변수 CORS_ALLOWED_ORIGINS 로 주입되며, 미지정 시 FRONTEND_URL 을 따른다.
     * (하드코딩하면 배포 도메인이 바뀔 때마다 이미지를 다시 빌드해야 한다)
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // [1] 기본 설정 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // [추가] 로그아웃 비활성화

                // [2] 세션 정책: STATELESS (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // [추가] 인증 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // [3] API 경로별 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // CORS Preflight 요청 허용
                        // /api/members/me 같은 인증 필요한 API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/members/**", "/api/workout/**", "/api/workout-programs/**", "/api/workoutroutine/**", "/api/workout-sessions/**", "/api/dashboard/**").authenticated()
                        // 그 외 모든 요청(로그인, 루트 등) 허용
                        .anyRequest().permitAll()
                )

                // [4] OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                );

        // [5] (신규) JWT 필터 추가
        //    : UsernamePasswordAuthenticationFilter 앞에 JwtAuthenticationFilter를 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}