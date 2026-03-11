package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import java.time.Instant;
import java.time.ZonedDateTime;

public interface RecentWorkoutProjection {
    Long getId();
    String getProgramName();
    Instant getStartTime();
    Long getDurationSeconds();
    Long getCompletedSets();
    Long getTotalSets();
}
