package com.fitlog.fitlogv2server.domain.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardStatsDto {

    private long totalWorkouts;
    private long totalDurationSeconds;
    private long totalCompletedSets;
    private double averageCompletionRate;
    private int currentStreak;
    private int weeklyWorkouts;
    private int monthlyWorkouts;
    private long longestWorkoutSeconds;
    private String favoriteBodyPart;

    private List<WeeklyProgressItem> weeklyProgress;
    private List<BodyPartStatItem> bodyPartStats;
    private List<MonthlyStatItem> monthlyStats;
    private List<RecentWorkoutItem> recentWorkouts;

    @Getter
    @Builder
    public static class WeeklyProgressItem {
        private String dayOfWeek;
        private int workoutCount;
        private long totalDurationSeconds;
    }

    @Getter
    @Builder
    public static class BodyPartStatItem {
        private String bodyPart;
        private int count;
        private int percentage;
    }

    @Getter
    @Builder
    public static class MonthlyStatItem {
        private int year;
        private int month;
        private int workoutCount;
        private long totalDurationSeconds;
    }

    @Getter
    @Builder
    public static class RecentWorkoutItem {
        private long id;
        private String programName;
        private String date;
        private long totalDurationSeconds;
        private int completedSets;
        private int totalSets;
    }
}
