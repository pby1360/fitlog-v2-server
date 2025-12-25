package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutPartRepository;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutPartRepository workoutPartRepository;

    public WorkoutPart findWorkoutPartByName(String name) {
        return workoutPartRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Workout part not found: " + name));
    }

    public Workout findWorkoutByName(String name) {
        return workoutRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Workout not found: " + name));
    }
}
