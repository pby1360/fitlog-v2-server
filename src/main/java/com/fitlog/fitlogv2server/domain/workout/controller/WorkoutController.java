package com.fitlog.fitlogv2server.domain.workout.controller;

import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.facade.WorkoutFacade;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workout")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final WorkoutFacade workoutFacade;

    @GetMapping("/parts")
    public ResponseEntity<List<WorkoutPartDto>> getWorkoutParts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workoutService.getWorkoutParts(userDetails.getId()));
    }

    @PostMapping("/parts")
    public ResponseEntity<Void> addWorkoutPart(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody WorkoutPartDto.Request request) {
        workoutFacade.addWorkoutPart(request, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/parts/{workoutPartId}")
    public ResponseEntity<Void> updateWorkoutPart(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long workoutPartId, @RequestBody WorkoutPartDto.Request request) {
        workoutFacade.updateWorkoutPart(workoutPartId, request, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/parts/{workoutPartId}")
    public ResponseEntity<Void> deleteWorkoutPart(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long workoutPartId) {
        workoutFacade.deleteWorkoutPart(workoutPartId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workouts")
    public ResponseEntity<List<WorkoutDto>> getWorkouts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workoutService.getWorkouts(userDetails.getId()));
    }

    @PostMapping("/workouts")
    public ResponseEntity<Void> addWorkout(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody WorkoutDto.Request request) {
        workoutFacade.addWorkout(request, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/workouts/{workoutId}")
    public ResponseEntity<Void> updateWorkout(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long workoutId, @RequestBody WorkoutDto.Request request) {
        workoutFacade.updateWorkout(workoutId, request, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/workouts/{workoutId}")
    public ResponseEntity<Void> deleteWorkout(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long workoutId) {
        workoutFacade.deleteWorkout(workoutId, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
