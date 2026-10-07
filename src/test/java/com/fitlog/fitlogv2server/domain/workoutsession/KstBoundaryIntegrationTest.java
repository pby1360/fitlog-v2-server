package com.fitlog.fitlogv2server.domain.workoutsession;

import com.fitlog.fitlogv2server.domain.member.dto.MemberResponseDto;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberService;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.MonthlyStatProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WeeklyProgressProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB 세션 시간대가 UTC(운영과 동일)여도 통계가 한국 날짜 기준으로 집계되는지 확인한다.
 * KST 00:00~09:00 은 UTC 로는 전날이므로 경계 오류가 드러나는 구간이다.
 */
class KstBoundaryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;
    @Autowired
    private MemberService memberService;
    @Autowired
    private TestFixtures fixtures;

    @Test
    void mondayEarlyMorningKst_isCountedAsMonday() {
        Member member = fixtures.member();
        // 2026-09-21 은 월요일. KST 00:30 = UTC 일요일 15:30
        ZonedDateTime mondayKst = ZonedDateTime.of(2026, 9, 21, 0, 30, 0, 0, AppTimeZone.KST);
        completedSession(member, mondayKst);

        List<WeeklyProgressProjection> rows = workoutSessionRepository.findWeeklyProgress(
                member.getId(), mondayKst.minusDays(1), mondayKst.plusDays(6));

        assertThat(rows).extracting(WeeklyProgressProjection::getDayOfWeek).containsExactly(1); // ISO 월요일
    }

    @Test
    void firstDayOfMonthEarlyMorningKst_isCountedInThatMonth() {
        Member member = fixtures.member();
        // KST 10/01 00:30 = UTC 09/30 15:30
        ZonedDateTime octoberFirstKst = ZonedDateTime.of(2026, 10, 1, 0, 30, 0, 0, AppTimeZone.KST);
        completedSession(member, octoberFirstKst);

        List<MonthlyStatProjection> rows = workoutSessionRepository.findMonthlyStats(member.getId(), octoberFirstKst.minusMonths(1));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getMonth()).isEqualTo(10);
    }

    @Test
    void streakDates_useKstDay() {
        Member member = fixtures.member();
        ZonedDateTime earlyMorningKst = ZonedDateTime.of(2026, 9, 22, 1, 0, 0, 0, AppTimeZone.KST);
        completedSession(member, earlyMorningKst);

        assertThat(workoutSessionRepository.findCompletedWorkoutDatesKst(member.getId())).containsExactly("2026-09-22");
    }

    @Test
    void memberCreatedAt_isShownAsKstDate() {
        Member member = fixtures.member();

        MemberResponseDto profile = memberService.getMyProfile(member.getId());

        assertThat(profile.getCreatedAt()).isEqualTo(LocalDate.now(AppTimeZone.KST).toString());
    }

    private void completedSession(Member member, ZonedDateTime startKst) {
        WorkoutProgram program = fixtures.emptyProgram(member);
        workoutSessionRepository.save(WorkoutSession.builder()
                .member(member)
                .workoutProgram(program)
                .startTime(startKst)
                .endTime(startKst.plusMinutes(30))
                .status(SessionStatus.COMPLETED)
                .build());
    }
}
