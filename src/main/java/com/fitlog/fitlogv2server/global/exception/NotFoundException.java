package com.fitlog.fitlogv2server.global.exception;

/**
 * 리소스가 없거나, 다른 회원의 비공개 리소스일 때 (404).
 * 두 경우를 구분하지 않아 다른 회원 리소스의 존재 여부가 드러나지 않게 한다.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
