package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutPartRepository workoutPartRepository;

    public List<WorkoutPartDto> getWorkoutParts(Long memberId) {
        List<WorkoutPart> workoutParts = workoutPartRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workoutParts.stream()
                .map(WorkoutPartDto::new)
                .collect(Collectors.toList());
    }

    public List<WorkoutDto> getWorkouts(Long memberId) {
        List<Workout> workouts = workoutRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workouts.stream()
                .map(WorkoutDto::new)
                .collect(Collectors.toList());
    }

    public WorkoutPart findWorkoutPartByName(String name) {
        return workoutPartRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
    }

    public Workout findWorkoutByName(String name) {
        return workoutRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
    }
}