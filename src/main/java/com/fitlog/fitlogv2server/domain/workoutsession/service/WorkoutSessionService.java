package com.fitlog.fitlogv2server.domain.workoutsession.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramExercise;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramPart;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgramSet;
import com.fitlog.fitlogv2server.domain.workoutprogram.repository.WorkoutProgramRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutProgramRepository workoutProgramRepository;

    @Transactional
    public WorkoutSession startSession(Member member, Long workoutProgramId) {
        WorkoutProgram workoutProgram = workoutProgramRepository.findById(workoutProgramId)
                .orElseThrow(() -> new IllegalArgumentException("Workout program not found"));

        WorkoutSession workoutSession = WorkoutSession.builder()
                .member(member)
                .workoutProgram(workoutProgram)
                .startTime(LocalDateTime.now())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        for (WorkoutProgramPart programPart : workoutProgram.getParts()) {
            for (WorkoutProgramExercise programExercise : programPart.getExercises()) {
                WorkoutSessionExercise sessionExercise = WorkoutSessionExercise.builder()
                        .workoutSession(workoutSession)
                        .workout(programExercise.getWorkout())
                        .order(programExercise.getOrder())
                        .build();
                workoutSession.addWorkoutSessionExercise(sessionExercise);

                for (WorkoutProgramSet programSet : programExercise.getSets()) {
                    WorkoutSessionSet sessionSet = WorkoutSessionSet.builder()
                            .workoutSessionExercise(sessionExercise)
                            .setNumber(programSet.getSetNumber())
                            .weight(programSet.getWeight())
                            .reps(programSet.getReps())
                            .restTime(programSet.getRestTime())
                            .memo(programSet.getMemo())
                            .completed(false)
                            .build();
                    sessionExercise.addWorkoutSessionSet(sessionSet);
                }
            }
        }

        return workoutSessionRepository.save(workoutSession);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getLatestInProgressSession(Long memberId) {
        return workoutSessionRepository.findLatestInProgressSessionByMemberId(memberId, SessionStatus.IN_PROGRESS);
    }
}
