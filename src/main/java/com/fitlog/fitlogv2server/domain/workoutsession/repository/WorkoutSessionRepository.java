package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    @Query("SELECT ws FROM WorkoutSession ws " +
            "LEFT JOIN FETCH ws.workoutProgram " +
            "LEFT JOIN FETCH ws.workoutSessionExercises wse " +
            "LEFT JOIN FETCH wse.workout " +
            "LEFT JOIN FETCH wse.workoutSessionSets " +
            "WHERE ws.member.id = :memberId AND ws.status IN (:statuses) " +
            "ORDER BY ws.id DESC")
    Optional<WorkoutSession> findLatestWorkoutSessionByMemberIdAndStatuses(@Param("memberId") Long memberId, @Param("statuses") Set<SessionStatus> statuses);

    Optional<WorkoutSession> findByIdAndMemberId(Long sessionId, Long memberId);

    @Query("SELECT ws FROM WorkoutSession ws " +
            "JOIN FETCH ws.workoutProgram " +
            "WHERE ws.member.id = :memberId AND ws.status = :status " +
            "ORDER BY ws.startTime DESC")
    Page<WorkoutSession> findAllByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") SessionStatus status, Pageable pageable);

    @Query("SELECT ws FROM WorkoutSession ws " +
            "LEFT JOIN FETCH ws.workoutProgram " +
            "LEFT JOIN FETCH ws.workoutSessionExercises wse " +
            "LEFT JOIN FETCH wse.workout " +
            "LEFT JOIN FETCH wse.workoutSessionSets " +
            "WHERE ws.id = :sessionId AND ws.member.id = :memberId")
    Optional<WorkoutSession> findDetailByIdAndMemberId(@Param("sessionId") Long sessionId, @Param("memberId") Long memberId);
}
