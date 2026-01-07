package com.fitlog.fitlogv2server.domain.workoutprogram.controller;

import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.facade.WorkoutProgramFacade;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workout-programs")
@RequiredArgsConstructor
public class WorkoutProgramController {

    private final WorkoutProgramFacade workoutProgramFacade;

    @PostMapping
    public ResponseEntity<Void> createWorkoutProgram(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody WorkoutProgramDto.Request requestDto) {
        workoutProgramFacade.createWorkoutProgram(userDetails.getId(), requestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WorkoutProgramDto.Response>> getWorkoutPrograms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<WorkoutProgramDto.Response> programs = workoutProgramFacade.findAllPrograms(userDetails.getId());
        return ResponseEntity.ok(programs);
    }
}
