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
        WorkoutProgram workoutProgram = WorkoutProgram.builder()
                .member(member)
                .name(requestDto.name())
                .description(requestDto.description())
                .build();

        addPartsToWorkoutProgram(workoutProgram, requestDto.parts());

        workoutProgramRepository.save(workoutProgram);
    }

    @Transactional
    public void updateWorkoutProgram(Long programId, WorkoutProgramDto.Request requestDto, Member member) {
        WorkoutProgram workoutProgram = workoutProgramRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("WorkoutProgram not found with id: " + programId));

        if (!workoutProgram.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("You do not have permission to update this workout program.");
        }

        workoutProgram.update(requestDto.name(), requestDto.description());

        // 기존 parts 삭제 후 새로 추가 (orphanRemoval = true 덕분에 자동으로 하위 엔티티 삭제)
        workoutProgram.getParts().clear();
        addPartsToWorkoutProgram(workoutProgram, requestDto.parts());
    }

    @Transactional
    public void deleteWorkoutProgram(Long programId, Member member) {
        WorkoutProgram workoutProgram = workoutProgramRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("WorkoutProgram not found with id: " + programId));

        if (!workoutProgram.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("You do not have permission to delete this workout program.");
        }

        workoutProgramRepository.delete(workoutProgram);
    }

    private void addPartsToWorkoutProgram(WorkoutProgram workoutProgram, List<WorkoutProgramDto.Request.PartDto> partDtos) {
        int partOrder = 0;
        for (WorkoutProgramDto.Request.PartDto partDto : partDtos) {
            WorkoutPart workoutPart = workoutService.findWorkoutPartById(partDto.workoutPartId());
            WorkoutProgramPart programPart = WorkoutProgramPart.builder()
                    .workoutProgram(workoutProgram)
                    .workoutPart(workoutPart)
                    .order(partOrder++)
                    .build();
            workoutProgram.getParts().add(programPart);

            int exerciseOrder = 0;
            for (WorkoutProgramDto.Request.ExerciseDto exerciseDto : partDto.exercises()) {
                Workout workout = workoutService.findWorkoutById(exerciseDto.workoutId());
                WorkoutProgramExercise programExercise = WorkoutProgramExercise.builder()
                        .workoutProgramPart(programPart)
                        .workout(workout)
                        .order(exerciseOrder++)
                        .build();
                programPart.getExercises().add(programExercise);

                for (WorkoutProgramDto.Request.SetDto setDto : exerciseDto.sets()) {
                    WorkoutProgramSet programSet = WorkoutProgramSet.builder()
                            .workoutProgramExercise(programExercise)
                            .setNumber(setDto.setNumber())
                            .weight(setDto.weight())
                            .reps(setDto.reps())
                            .restTime(setDto.restTime())
                            .memo(setDto.memo())
                            .build();
                    programExercise.getSets().add(programSet);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<WorkoutProgramDto.Response> findAllPrograms(Long memberId) {
        return workoutProgramRepository.findAllByMemberId(memberId).stream()
                .map(WorkoutProgramDto.Response::new)
                .collect(Collectors.toList());
    }
}

