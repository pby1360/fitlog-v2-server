package com.fitlog.fitlogv2server.domain.workoutprogram.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramExercise;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramPart;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramSet;
import com.fitlog.fitlogv2server.domain.workoutprogram.repository.WorkoutProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutProgramService {

    private final WorkoutProgramRepository workoutProgramRepository;
    private final WorkoutService workoutService;

    @Transactional
    public void createWorkoutProgram(WorkoutProgramDto.Request requestDto, Member member) {
        // ... (기존 로직)
    }

    @Transactional(readOnly = true)
    public List<WorkoutProgramDto.Response> findAllPrograms(Long memberId) {
        // TODO: memberId로 필터링하는 로직 추가 필요. 현재는 모든 프로그램을 반환
        return workoutProgramRepository.findAll().stream()
                .map(WorkoutProgramDto.Response::new)
                .collect(Collectors.toList());
    }
}

