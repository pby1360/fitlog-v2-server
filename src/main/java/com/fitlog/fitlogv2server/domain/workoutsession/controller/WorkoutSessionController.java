package com.fitlog.fitlogv2server.domain.workoutsession.controller;

import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.facade.WorkoutSessionFacade;
import com.fitlog.fitlogv2server.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
}
