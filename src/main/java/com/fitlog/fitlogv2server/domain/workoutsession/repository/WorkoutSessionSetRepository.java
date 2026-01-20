package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionSetRepository extends JpaRepository<WorkoutSessionSet, Long> {
}
