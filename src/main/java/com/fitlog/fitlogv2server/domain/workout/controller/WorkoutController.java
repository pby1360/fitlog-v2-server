package com.fitlog.fitlogv2server.domain.workout.controller;

import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workout")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @GetMapping("/parts")
    public ResponseEntity<List<WorkoutPartDto>> getWorkoutParts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workoutService.getWorkoutParts(userDetails.getId()));
    }

    @GetMapping("/list")
    public ResponseEntity<List<WorkoutDto>> getWorkouts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workoutService.getWorkouts(userDetails.getId()));
    }
}
