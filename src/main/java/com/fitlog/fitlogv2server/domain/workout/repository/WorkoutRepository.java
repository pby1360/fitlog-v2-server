package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    Optional<Workout> findByName(String name);
}
