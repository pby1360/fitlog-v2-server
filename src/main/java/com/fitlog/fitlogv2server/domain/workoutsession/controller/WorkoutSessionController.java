package com.fitlog.fitlogv2server.domain.workoutsession.controller;

import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.facade.WorkoutSessionFacade;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/workout-sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {

    private final WorkoutSessionFacade workoutSessionFacade;

    @PostMapping
    public ResponseEntity<WorkoutSessionDto.Response> startSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody WorkoutSessionDto.StartRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.startSession(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<WorkoutSessionDto.Response> getLatestInProgressSession(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return workoutSessionFacade.getLatestInProgressSession(userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{sessionId}/complete-set")
    public ResponseEntity<WorkoutSessionDto.Response> completeSet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.CompleteSetRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.completeSet(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sessionId}/pause")
    public ResponseEntity<WorkoutSessionDto.Response> pauseSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        WorkoutSessionDto.Response response = workoutSessionFacade.pauseSession(userDetails.getId(), sessionId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sessionId}/resume")
    public ResponseEntity<WorkoutSessionDto.Response> resumeSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        WorkoutSessionDto.Response response = workoutSessionFacade.resumeSession(userDetails.getId(), sessionId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sessionId}/skip-exercise")
    public ResponseEntity<WorkoutSessionDto.Response> skipExercise(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.SkipExerciseRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.skipExercise(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sessionId}/reorder-exercises")
    public ResponseEntity<WorkoutSessionDto.Response> reorderExercises(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.ReorderExercisesRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.reorderExercises(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/exercises")
    public ResponseEntity<WorkoutSessionDto.Response> addExercise(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.AddExerciseRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.addExercise(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}/exercises/{exerciseId}")
    public ResponseEntity<WorkoutSessionDto.Response> removeExercise(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        WorkoutSessionDto.Response response = workoutSessionFacade.removeExercise(userDetails.getId(), sessionId, exerciseId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<WorkoutSessionDto.Response> endSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.EndRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.endSession(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<WorkoutSessionDto.LogPageResponse> getWorkoutLog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WorkoutSessionDto.LogPageResponse response = workoutSessionFacade.getWorkoutLog(userDetails.getId(), pageable, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/{sessionId}")
    public ResponseEntity<WorkoutSessionDto.Response> getSessionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        WorkoutSessionDto.Response response = workoutSessionFacade.getSessionDetail(userDetails.getId(), sessionId);
        return ResponseEntity.ok(response);
    }

}
