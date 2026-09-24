package com.fitlog.fitlogv2server.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * 모든 API 오류를 {code, message, requestId} 형식으로 응답한다.
 * - 400: 요청 값 오류 / 404: 없음 또는 다른 회원의 리소스 / 409: 현재 상태와 충돌 / 500: 예상하지 못한 오류
 * - 500 응답에는 내부 원인(SQL, 토큰, 개인정보 등)을 싣지 않고 로그로만 남긴다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseBody> handleIllegalArgument(IllegalArgumentException ex) {
        return respond(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseBody> handleNotFound(NotFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseBody> handleConflict(ConflictException ex) {
        return respond(ErrorCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseBody> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return respond(ex.getStatusCode(), message);
    }

    // 요청 DTO의 Bean Validation 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseBody> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(ErrorCode.VALIDATION_FAILED, message.isEmpty() ? "요청 값이 올바르지 않습니다." : message);
    }

    // JSON 형식 오류, 타입 불일치(예: 숫자 자리에 문자열)
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseBody> handleNotReadable(Exception ex) {
        return respond(ErrorCode.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
    }

    // DB 제약 위반 (동시 요청으로 인한 중복 등). 내부 SQL/제약 정보는 응답하지 않고 로그로만 남긴다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseBody> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(ErrorCode.CONFLICT, "요청이 현재 데이터와 충돌합니다. 새로고침 후 다시 시도해주세요.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseBody> handleUnexpected(Exception ex) {
        // 스프링 MVC 표준 예외(405, 404 경로 없음, 파라미터 누락 등)는 자체 상태 코드를 유지한다
        if (ex instanceof ErrorResponse errorResponse) {
            return respond(errorResponse.getStatusCode(), errorResponse.getBody().getDetail() != null
                    ? errorResponse.getBody().getDetail() : "요청을 처리할 수 없습니다.");
        }
        log.error("Unhandled exception", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private ResponseEntity<ErrorResponseBody> respond(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ErrorResponseBody.of(ErrorCode.fromStatus(status), message));
    }

    private ResponseEntity<ErrorResponseBody> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(ErrorResponseBody.of(code, message));
    }
}
