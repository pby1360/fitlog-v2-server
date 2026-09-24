package com.fitlog.fitlogv2server.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.global.exception.ErrorCode;
import com.fitlog.fitlogv2server.global.exception.ErrorResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 인증되지 않은 사용자가 보호된 리소스에 접근할 때 공통 오류 형식으로 401 응답
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseBody.of(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
