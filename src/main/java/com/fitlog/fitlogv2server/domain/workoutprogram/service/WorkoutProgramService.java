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

import java.util.concurrent.atomic.AtomicInteger;

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

        AtomicInteger partOrder = new AtomicInteger(1);
        requestDto.workoutParts().forEach(partDto -> {
            WorkoutPart workoutPart = workoutService.findWorkoutPartByName(partDto.name());
            WorkoutProgramPart programPart = WorkoutProgramPart.builder()
                    .workoutProgram(workoutProgram)
                    .workoutPart(workoutPart)
                    .order(partOrder.getAndIncrement())
                    .build();
            workoutProgram.getParts().add(programPart);

            AtomicInteger exerciseOrder = new AtomicInteger(1);
            partDto.workoutExercises().forEach(exerciseDto -> {
                Workout workout = workoutService.findWorkoutByName(exerciseDto.name());
                WorkoutProgramExercise programExercise = WorkoutProgramExercise.builder()
                        .workoutProgramPart(programPart)
                        .workout(workout)
                        .order(exerciseOrder.getAndIncrement())
                        .build();
                programPart.getExercises().add(programExercise);

                exerciseDto.workoutSets().forEach(setDto -> {
                    WorkoutProgramSet programSet = WorkoutProgramSet.builder()
                            .workoutProgramExercise(programExercise)
                            .setNumber(setDto.setNumber())
                            .weight(setDto.weight())
                            .reps(setDto.reps())
                            .restTime(setDto.restTime())
                            .memo(setDto.memo())
                            .build();
                    programExercise.getSets().add(programSet);
                });
            });
        });

        workoutProgramRepository.save(workoutProgram);
    }
}
