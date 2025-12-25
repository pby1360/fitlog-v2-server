package com.fitlog.fitlogv2server.domain.workoutprogram.repository;

import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {
}
