package com.fitlog.fitlogv2server.domain.workoutsession.controller;

import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.facade.WorkoutSessionFacade;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<WorkoutSessionDto.Response> endSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestBody WorkoutSessionDto.EndRequest request) {
        WorkoutSessionDto.Response response = workoutSessionFacade.endSession(userDetails.getId(), sessionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<WorkoutSessionDto.LogSummaryResponse>> getWorkoutLog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkoutSessionDto.LogSummaryResponse> response = workoutSessionFacade.getWorkoutLog(userDetails.getId(), pageable);
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
