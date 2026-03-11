package com.fitlog.fitlogv2server.domain.workoutsession.repository;

public interface MonthlyStatProjection {
    Integer getYear();
    Integer getMonth();
    Long getWorkoutCount();
    Long getTotalDurationSeconds();
}
