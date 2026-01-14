package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
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
@Transactional
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutPartRepository workoutPartRepository;

    @Transactional(readOnly = true)
    public List<WorkoutPartDto> getWorkoutParts(Long memberId) {
        List<WorkoutPart> workoutParts = workoutPartRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workoutParts.stream()
                .map(WorkoutPartDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> getWorkouts(Long memberId) {
        List<Workout> workouts = workoutRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workouts.stream()
                .map(WorkoutDto::new)
                .collect(Collectors.toList());
    }

    public void addWorkoutPart(WorkoutPartDto.Request request, Member member) {
        workoutPartRepository.save(WorkoutPart.builder()
                .name(request.getName())
                .member(member)
                .build());
    }

    public void updateWorkoutPart(Long workoutPartId, WorkoutPartDto.Request request, Member member) {
        WorkoutPart workoutPart = workoutPartRepository.findById(workoutPartId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
        if (!workoutPart.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        workoutPart.updateName(request.getName());
    }

    public void deleteWorkoutPart(Long workoutPartId, Member member) {
        WorkoutPart workoutPart = workoutPartRepository.findById(workoutPartId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
        if (!workoutPart.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        workoutRepository.deleteAllByWorkoutPartId(workoutPartId);
        workoutPartRepository.delete(workoutPart);
    }

    public void addWorkout(WorkoutDto.Request request, Member member) {
        WorkoutPart workoutPart = findWorkoutPartById(request.getWorkoutPartId());
        workoutRepository.save(Workout.builder()
                .name(request.getName())
                .workoutPart(workoutPart)
                .member(member)
                .build());
    }

    public void updateWorkout(Long workoutId, WorkoutDto.Request request, Member member) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
        if (!workout.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        WorkoutPart workoutPart = findWorkoutPartById(request.getWorkoutPartId());
        workout.update(request.getName(), workoutPart);
    }

    public void deleteWorkout(Long workoutId, Member member) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
        if (!workout.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        workoutRepository.delete(workout);
    }

    @Transactional(readOnly = true)
    public WorkoutPart findWorkoutPartByName(String name) {
        return workoutPartRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
    }

    @Transactional(readOnly = true)
    public Workout findWorkoutByName(String name) {
        return workoutRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
    }

    @Transactional(readOnly = true)
    public WorkoutPart findWorkoutPartById(Long id) {
        return workoutPartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
    }

    @Transactional(readOnly = true)
    public Workout findWorkoutById(Long id) {
        return workoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
    }
}