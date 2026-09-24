package com.fitlog.fitlogv2server.global.common;

/**
 * 요청 입력값 허용 범위. 요청 DTO의 Bean Validation과 DB CHECK 제약(V3)이 같은 기준을 사용한다.
 */
public final class ValidationLimits {

    public static final int MAX_NAME = 50;          // 프로그램·운동·부위 이름
    public static final int MAX_DESCRIPTION = 255;
    public static final int MAX_MEMO = 255;

    public static final int MAX_PARTS = 20;         // 프로그램 한 개의 부위 수
    public static final int MAX_EXERCISES = 50;     // 프로그램 부위/세션 한 개의 운동 수
    public static final int MAX_SETS = 50;          // 운동 한 개의 세트 수

    public static final String MAX_WEIGHT = "2000"; // kg (DecimalMax 는 문자열)
    public static final int MAX_REPS = 1000;
    public static final int MAX_REST_SECONDS = 3600;

    private ValidationLimits() {
    }
}
