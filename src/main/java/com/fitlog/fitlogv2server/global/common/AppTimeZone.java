package com.fitlog.fitlogv2server.global.common;

import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 시간대 처리 정책.
 * <p>
 * 모든 시각 데이터는 UTC로 저장하고, 사용자에게 표시하거나 날짜 단위로 조회할 때만
 * 클라이언트(사용자)의 시간대로 변환한다. 글로벌 서비스를 위해 서버의 로컬 시간대에
 * 의존하지 않는다.
 */
public final class AppTimeZone {

    /** 저장 기준 시간대. 모든 타임스탬프는 UTC로 보관한다. */
    public static final ZoneId STORAGE_ZONE = ZoneOffset.UTC;

    private AppTimeZone() {
    }

    /**
     * 클라이언트가 전달한 IANA 시간대 문자열(예: "Asia/Seoul")을 안전하게 {@link ZoneId}로 변환한다.
     * 값이 없거나 유효하지 않으면 UTC로 처리한다.
     */
    public static ZoneId resolveOrUtc(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }
}
