package com.fitlog.fitlogv2server.global.exception;

/**
 * 현재 리소스 상태와 요청이 충돌할 때 (409).
 * 예: 이미 진행 중인 세션이 있는데 새 세션 시작, 종료된 세션 수정.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
