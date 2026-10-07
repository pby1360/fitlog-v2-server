package com.fitlog.fitlogv2server.domain.workoutsession.entity;

import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutSessionTest {

    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 9, 24, 10, 0, 0, 0, AppTimeZone.KST);

    private WorkoutSession activeSession() {
        return WorkoutSession.builder().startTime(T0).status(SessionStatus.IN_PROGRESS).build();
    }

    @Test
    void finishWhilePaused_excludesLastPausedInterval() {
        WorkoutSession session = activeSession();
        session.pause(T0.plusMinutes(10));

        session.finish(SessionStatus.COMPLETED, T0.plusMinutes(40));

        // 10분 운동 + 30분 정지 후 종료 → 실제 운동 시간 10분
        assertThat(session.getDurationSeconds()).isEqualTo(600);
        assertThat(session.getTotalPausedSeconds()).isEqualTo(1800);
        assertThat(session.getLastPausedAt()).isNull();
    }

    @Test
    void durationExcludesAllPausedIntervals() {
        WorkoutSession session = activeSession();
        session.pause(T0.plusMinutes(20));
        session.resume(T0.plusMinutes(30));

        session.finish(SessionStatus.COMPLETED, T0.plusMinutes(60));

        // 60분 세션 - 10분 정지 = 50분 (상세/목록 모두 이 값을 사용)
        assertThat(session.getDurationSeconds()).isEqualTo(3000);
    }

    @Test
    void finish_isIdempotentForSameStatus() {
        WorkoutSession session = activeSession();
        session.finish(SessionStatus.COMPLETED, T0.plusMinutes(30));

        session.finish(SessionStatus.COMPLETED, T0.plusMinutes(90));

        assertThat(session.getEndTime()).isEqualTo(T0.plusMinutes(30));
    }

    @Test
    void finish_rejectsChangingEndedStatus() {
        WorkoutSession session = activeSession();
        session.finish(SessionStatus.CANCELLED, T0.plusMinutes(5));

        assertThatThrownBy(() -> session.finish(SessionStatus.COMPLETED, T0.plusMinutes(6)))
                .isInstanceOf(ConflictException.class);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
    }

    @Test
    void finish_rejectsNonTerminalStatus() {
        WorkoutSession session = activeSession();

        assertThatThrownBy(() -> session.finish(SessionStatus.IN_PROGRESS, T0.plusMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> session.finish(null, T0.plusMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endedSession_cannotBePausedOrResumed() {
        WorkoutSession session = activeSession();
        session.finish(SessionStatus.COMPLETED, T0.plusMinutes(30));

        assertThatThrownBy(() -> session.pause(T0.plusMinutes(31))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> session.resume(T0.plusMinutes(31))).isInstanceOf(ConflictException.class);
    }

    @Test
    void pauseAndResume_areIdempotent() {
        WorkoutSession session = activeSession();
        session.pause(T0.plusMinutes(10));
        session.pause(T0.plusMinutes(15)); // 재시도: 최초 정지 시각 유지

        session.resume(T0.plusMinutes(20));
        session.resume(T0.plusMinutes(25)); // 재시도: 추가 누적 없음

        assertThat(session.getTotalPausedSeconds()).isEqualTo(600);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void completeSet_isIdempotentForSameValues_andRejectsDifferentValues() {
        WorkoutSessionSet set = WorkoutSessionSet.builder().setNumber(1).reps(10).restTime(60).completed(false).build();
        set.completeSet(60.0, 10, null);
        ZonedDateTime completedAt = set.getCompletedAt();

        set.completeSet(60.0, 10, null);
        assertThat(set.getCompletedAt()).isEqualTo(completedAt);

        assertThatThrownBy(() -> set.completeSet(80.0, 10, null)).isInstanceOf(ConflictException.class);
        assertThat(set.getActualWeight()).isEqualTo(60.0);
    }
}
