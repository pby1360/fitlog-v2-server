package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionExerciseRepository extends JpaRepository<WorkoutSessionExercise, Long> {
}
