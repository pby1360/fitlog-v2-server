package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findAllByMemberIdOrMemberIsNull(Long memberId);
    Optional<Workout> findByName(String name);
    void deleteAllByWorkoutPartId(Long workoutPartId);
}
