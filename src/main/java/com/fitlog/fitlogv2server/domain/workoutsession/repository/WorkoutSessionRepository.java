package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Timestamp;

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
            "JOIN FETCH ws.workoutProgram " +
            "WHERE ws.member.id = :memberId AND ws.status = :status " +
            "AND ws.startTime >= :startDate AND ws.startTime < :endDate " +
            "ORDER BY ws.startTime DESC")
    Page<WorkoutSession> findAllByMemberIdAndStatusBetween(@Param("memberId") Long memberId, @Param("status") SessionStatus status,
            @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate, Pageable pageable);

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

    @Query(value = "SELECT COALESCE(SUM(" +
            "CASE WHEN ws.start_time IS NOT NULL AND ws.end_time IS NOT NULL " +
            "THEN EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) - COALESCE(ws.total_paused_seconds, 0) " +
            "ELSE 0 END), 0) " +
            "FROM workout_session ws WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED' " +
            "AND ws.start_time >= :startDate AND ws.start_time < :endDate",
            nativeQuery = true)
    Long sumDurationSecondsByMemberIdBetween(@Param("memberId") Long memberId,
            @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED' AND wss.completed = true",
            nativeQuery = true)
    Long sumCompletedSetsByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED' AND wss.completed = true " +
            "AND ws.start_time >= :startDate AND ws.start_time < :endDate",
            nativeQuery = true)
    Long sumCompletedSetsByMemberIdBetween(@Param("memberId") Long memberId,
            @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'",
            nativeQuery = true)
    Long sumTotalSetsByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT COUNT(*) FROM workout_session_set wss " +
            "JOIN workout_session_exercise wse ON wss.workout_session_exercise_id = wse.id " +
            "JOIN workout_session ws ON wse.workout_session_id = ws.id " +
            "WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED' " +
            "AND ws.start_time >= :startDate AND ws.start_time < :endDate",
            nativeQuery = true)
    Long sumTotalSetsByMemberIdBetween(@Param("memberId") Long memberId,
            @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    // --- Dashboard Stats Queries ---

    @Query(value = "SELECT COUNT(*) FROM workout_session WHERE member_id = :memberId AND status = 'COMPLETED'",
            nativeQuery = true)
    Long countCompleted(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT AVG(CAST(completed_count AS FLOAT) / NULLIF(total_count, 0))
            FROM (
                SELECT ws.id,
                    COUNT(CASE WHEN wss.completed = true THEN 1 END) AS completed_count,
                    COUNT(wss.id) AS total_count
                FROM workout_session ws
                JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
                JOIN workout_session_set wss ON wse.id = wss.workout_session_exercise_id
                WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
                GROUP BY ws.id
            ) sub
            """, nativeQuery = true)
    Double averageCompletionRate(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT COALESCE(MAX(
                EXTRACT(EPOCH FROM (end_time - start_time)) - COALESCE(total_paused_seconds, 0)
            ), 0)
            FROM workout_session
            WHERE member_id = :memberId AND status = 'COMPLETED'
            """, nativeQuery = true)
    Long maxDurationSeconds(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT COUNT(*) FROM workout_session
            WHERE member_id = :memberId AND status = 'COMPLETED'
            AND start_time >= :from AND start_time < :to
            """, nativeQuery = true)
    Long countCompletedBetween(@Param("memberId") Long memberId,
            @Param("from") ZonedDateTime from,
            @Param("to") ZonedDateTime to);

    @Query(value = "SELECT start_time FROM workout_session WHERE member_id = :memberId AND status = 'COMPLETED'",
            nativeQuery = true)
    List<Timestamp> findCompletedStartTimes(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT
                EXTRACT(ISODOW FROM start_time)::int AS dayOfWeek,
                COUNT(*) AS workoutCount,
                COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time)) - COALESCE(total_paused_seconds, 0)), 0) AS totalDurationSeconds
            FROM workout_session
            WHERE member_id = :memberId AND status = 'COMPLETED'
            AND start_time >= :from AND start_time < :to
            GROUP BY EXTRACT(ISODOW FROM start_time)
            """, nativeQuery = true)
    List<WeeklyProgressProjection> findWeeklyProgress(@Param("memberId") Long memberId,
            @Param("from") ZonedDateTime from,
            @Param("to") ZonedDateTime to);

    @Query(value = """
            SELECT wp.name AS bodyPart, COUNT(wp.id) AS count
            FROM workout_session ws
            JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            JOIN workout w ON wse.workout_id = w.id
            JOIN workout_part wp ON w.workout_part_id = wp.id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY wp.name
            ORDER BY count DESC
            """, nativeQuery = true)
    List<BodyPartStatProjection> findBodyPartStats(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT wp.name
            FROM workout_session ws
            JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            JOIN workout w ON wse.workout_id = w.id
            JOIN workout_part wp ON w.workout_part_id = wp.id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY wp.name
            ORDER BY COUNT(wp.id) DESC
            LIMIT 1
            """, nativeQuery = true)
    String findFavoriteBodyPart(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT
                EXTRACT(YEAR FROM start_time)::int AS year,
                EXTRACT(MONTH FROM start_time)::int AS month,
                COUNT(*) AS workoutCount,
                COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time)) - COALESCE(total_paused_seconds, 0)), 0) AS totalDurationSeconds
            FROM workout_session
            WHERE member_id = :memberId AND status = 'COMPLETED' AND start_time >= :from
            GROUP BY year, month
            ORDER BY year ASC, month ASC
            """, nativeQuery = true)
    List<MonthlyStatProjection> findMonthlyStats(@Param("memberId") Long memberId,
            @Param("from") ZonedDateTime from);

    @Query(value = """
            SELECT
                ws.id,
                wp.name AS programName,
                ws.start_time AS startTime,
                (EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) - COALESCE(ws.total_paused_seconds, 0)) AS durationSeconds,
                COUNT(CASE WHEN wss.completed = true THEN 1 END) AS completedSets,
                COUNT(wss.id) AS totalSets
            FROM workout_session ws
            LEFT JOIN workout_program wp ON ws.workout_program_id = wp.id
            LEFT JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            LEFT JOIN workout_session_set wss ON wse.id = wss.workout_session_exercise_id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY ws.id, wp.name, ws.start_time, ws.end_time, ws.total_paused_seconds
            ORDER BY ws.start_time DESC
            LIMIT 3
            """, nativeQuery = true)
    List<RecentWorkoutProjection> findRecentCompleted(@Param("memberId") Long memberId);
}
