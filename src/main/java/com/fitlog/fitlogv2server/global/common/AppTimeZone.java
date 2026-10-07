package com.fitlog.fitlogv2server.global.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 애플리케이션 전역에서 사용하는 기준 시간대(KST).
 * 세션 시각 저장(쓰기)과 대시보드 집계(읽기)가 동일한 존을 쓰도록 통일한다.
 */
public final class AppTimeZone {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 저장된 시각을 한국 날짜(YYYY-MM-DD)로 변환한다. null 이면 null.
    public static String toKstDateString(Instant instant) {
        return instant != null ? LocalDate.ofInstant(instant, KST).toString() : null;
    }

    private AppTimeZone() {
    }
}
