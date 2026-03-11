package com.fitlog.fitlogv2server.domain.dashboard.service;

import com.fitlog.fitlogv2server.domain.dashboard.dto.DashboardStatsDto;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.BodyPartStatProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.MonthlyStatProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.RecentWorkoutProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WeeklyProgressProjection;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WorkoutSessionRepository workoutSessionRepository;

    public DashboardStatsDto getStats(Long memberId) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        ZonedDateTime weekStartZdt = weekStart.atStartOfDay(zone);
        ZonedDateTime weekEndZdt = weekStart.plusDays(7).atStartOfDay(zone);

        YearMonth thisMonth = YearMonth.now(zone);
        ZonedDateTime monthStartZdt = thisMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime monthEndZdt = thisMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone);

        YearMonth threeMonthsAgo = thisMonth.minusMonths(2);
        ZonedDateTime threeMonthsStartZdt = threeMonthsAgo.atDay(1).atStartOfDay(zone);

        Long totalDuration = workoutSessionRepository.sumDurationSecondsByMemberId(memberId);
        Long totalCompletedSets = workoutSessionRepository.sumCompletedSetsByMemberId(memberId);
        Double avgRate = workoutSessionRepository.averageCompletionRate(memberId);
        Long longestWorkout = workoutSessionRepository.maxDurationSeconds(memberId);

        return DashboardStatsDto.builder()
                .totalWorkouts(workoutSessionRepository.countCompleted(memberId))
                .totalDurationSeconds(totalDuration != null ? totalDuration : 0L)
                .totalCompletedSets(totalCompletedSets != null ? totalCompletedSets : 0L)
                .averageCompletionRate(avgRate != null ? Math.round(avgRate * 10.0) / 10.0 : 0.0)
                .currentStreak(calculateStreak(memberId, zone))
                .weeklyWorkouts(workoutSessionRepository.countCompletedBetween(memberId, weekStartZdt, weekEndZdt).intValue())
                .monthlyWorkouts(workoutSessionRepository.countCompletedBetween(memberId, monthStartZdt, monthEndZdt).intValue())
                .longestWorkoutSeconds(longestWorkout != null ? longestWorkout : 0L)
                .favoriteBodyPart(workoutSessionRepository.findFavoriteBodyPart(memberId))
                .weeklyProgress(buildWeeklyProgress(memberId, weekStartZdt, weekEndZdt))
                .bodyPartStats(buildBodyPartStats(memberId))
                .monthlyStats(buildMonthlyStats(memberId, threeMonthsStartZdt))
                .recentWorkouts(buildRecentWorkouts(memberId, zone))
                .build();
    }

    private int calculateStreak(Long memberId, ZoneId zone) {
        List<Timestamp> startTimes = workoutSessionRepository.findCompletedStartTimes(memberId);
        if (startTimes == null || startTimes.isEmpty()) return 0;

        Set<LocalDate> dates = startTimes.stream()
                .map(ts -> {
                    // Convert SQL Timestamp -> Instant -> ZonedDateTime in the requested zone
                    return ts.toInstant().atZone(zone).toLocalDate();
                })
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(zone);
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(cursor)) return 0;

        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<DashboardStatsDto.WeeklyProgressItem> buildWeeklyProgress(
            Long memberId, ZonedDateTime from, ZonedDateTime to) {
        List<WeeklyProgressProjection> rows = workoutSessionRepository.findWeeklyProgress(memberId, from, to);
        Map<Integer, WeeklyProgressProjection> byDay = rows.stream()
                .collect(Collectors.toMap(WeeklyProgressProjection::getDayOfWeek, r -> r));

        String[] names = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        List<DashboardStatsDto.WeeklyProgressItem> result = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            WeeklyProgressProjection row = byDay.get(i);
            result.add(DashboardStatsDto.WeeklyProgressItem.builder()
                    .dayOfWeek(names[i - 1])
                    .workoutCount(row != null ? row.getWorkoutCount().intValue() : 0)
                    .totalDurationSeconds(row != null ? row.getTotalDurationSeconds() : 0L)
                    .build());
        }
        return result;
    }

    private List<DashboardStatsDto.BodyPartStatItem> buildBodyPartStats(Long memberId) {
        List<BodyPartStatProjection> rows = workoutSessionRepository.findBodyPartStats(memberId);
        long total = rows.stream().mapToLong(BodyPartStatProjection::getCount).sum();
        return rows.stream()
                .limit(4)
                .map(r -> DashboardStatsDto.BodyPartStatItem.builder()
                        .bodyPart(r.getBodyPart())
                        .count(r.getCount().intValue())
                        .percentage(total == 0 ? 0 : (int) Math.round(r.getCount() * 100.0 / total))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDto.MonthlyStatItem> buildMonthlyStats(Long memberId, ZonedDateTime from) {
        return workoutSessionRepository.findMonthlyStats(memberId, from).stream()
                .map(r -> DashboardStatsDto.MonthlyStatItem.builder()
                        .year(r.getYear())
                        .month(r.getMonth())
                        .workoutCount(r.getWorkoutCount().intValue())
                        .totalDurationSeconds(r.getTotalDurationSeconds())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDto.RecentWorkoutItem> buildRecentWorkouts(Long memberId, ZoneId zone) {
        return workoutSessionRepository.findRecentCompleted(memberId).stream()
                .map(r -> DashboardStatsDto.RecentWorkoutItem.builder()
                        .id(r.getId())
                        .programName(r.getProgramName())
                        .date(r.getStartTime().atZone(zone).toLocalDate().toString())
                        .totalDurationSeconds(r.getDurationSeconds() != null ? r.getDurationSeconds() : 0L)
                        .completedSets(r.getCompletedSets() != null ? r.getCompletedSets().intValue() : 0)
                        .totalSets(r.getTotalSets() != null ? r.getTotalSets().intValue() : 0)
                        .build())
                .collect(Collectors.toList());
    }
}
