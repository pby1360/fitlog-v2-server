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

    @Query(value = "SELECT COALESCE(SUM(" +
            "CASE WHEN ws.start_time IS NOT NULL AND ws.end_time IS NOT NULL " +
            "THEN EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) - COALESCE(ws.total_paused_seconds, 0) " +
            "ELSE 0 END), 0) " +
            "FROM workout_session ws WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'",
            nativeQuery = true)
    Long sumDurationSecondsByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED' AND wss.completed = true",
            nativeQuery = true)
    Long sumCompletedSetsByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'",
            nativeQuery = true)
    Long sumTotalSetsByMemberId(@Param("memberId") Long memberId);
}
