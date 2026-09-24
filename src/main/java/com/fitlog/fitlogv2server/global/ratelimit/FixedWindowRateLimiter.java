package com.fitlog.fitlogv2server.global.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 키(회원/IP)별 고정 구간 요청 수 제한. 인스턴스 메모리에만 저장한다.
 * - 인스턴스가 여러 개면 제한이 인스턴스 수만큼 느슨해지고, 재시작 시 초기화된다 (과다 요청 완화 목적).
 * - 오래된 키는 주기적으로 정리해 메모리가 계속 늘지 않게 한다.
 */
public class FixedWindowRateLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private static final class Window {
        long startMillis;
        int count;

        Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private static final int CLEANUP_EVERY_N_CALLS = 1000;

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private int callsSinceCleanup;

    public FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public Decision tryAcquire(String key) {
        long now = clock.millis();
        maybeCleanup(now);

        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.startMillis >= windowMillis) {
                window.startMillis = now;
                window.count = 0;
            }
            if (window.count >= limit) {
                long retryAfterMillis = window.startMillis + windowMillis - now;
                return new Decision(false, Math.max(1, (retryAfterMillis + 999) / 1000));
            }
            window.count++;
            return new Decision(true, 0);
        }
    }

    private void maybeCleanup(long now) {
        synchronized (this) {
            if (++callsSinceCleanup < CLEANUP_EVERY_N_CALLS) {
                return;
            }
            callsSinceCleanup = 0;
        }
        windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis >= 2 * windowMillis);
    }
}
