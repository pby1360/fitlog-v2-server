package com.fitlog.fitlogv2server.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fitlog.fitlogv2server.global.security.service.CustomOAuth2UserService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.client-url}")
    private String clientUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 인증 실패: {}", exception.getMessage());

        String targetUrl = UriComponentsBuilder.fromUriString(clientUrl + "/auth/callback")
                .queryParam("error", "oauth_error")
                .queryParam("message", userMessage(exception))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private static String userMessage(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String code = oauth2Exception.getError().getErrorCode();
            if (CustomOAuth2UserService.ERROR_EMAIL_REGISTERED_WITH_OTHER_PROVIDER.equals(code)) {
                String provider = oauth2Exception.getError().getDescription();
                return "이미 " + (provider != null ? provider : "다른 소셜") + " 계정으로 가입된 이메일입니다. 해당 계정으로 로그인해주세요.";
            }
        }
        return "로그인 중 오류가 발생했습니다";
    }
}
