package com.fitlog.fitlogv2server.global.exception;

import com.fitlog.fitlogv2server.global.logging.RequestIdFilter;
import org.slf4j.MDC;

/**
 * 모든 API 오류의 공통 응답 형식.
 * requestId 는 서버 로그와 같은 값이므로 사용자 문의 시 해당 요청의 로그를 찾을 수 있다.
 */
public record ErrorResponseBody(String code, String message, String requestId) {

    public static ErrorResponseBody of(ErrorCode code, String message) {
        return new ErrorResponseBody(code.name(), message, MDC.get(RequestIdFilter.MDC_KEY));
    }
}
