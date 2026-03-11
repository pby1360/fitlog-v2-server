package com.fitlog.fitlogv2server.domain.workoutsession.repository;

public interface WeeklyProgressProjection {
    Integer getDayOfWeek();
    Long getWorkoutCount();
    Long getTotalDurationSeconds();
}
