package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutPartRepository extends JpaRepository<WorkoutPart, Long> {
    List<WorkoutPart> findAllByMemberIdOrMemberIsNull(Long memberId);
    Optional<WorkoutPart> findByName(String name);
}
