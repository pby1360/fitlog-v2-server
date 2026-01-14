package com.fitlog.fitlogv2server.domain.workoutprogram.repository;

import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {
    List<WorkoutProgram> findAllByMemberId(Long memberId);
}
