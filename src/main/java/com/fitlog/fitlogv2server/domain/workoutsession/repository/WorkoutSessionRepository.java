package com.fitlog.fitlogv2server.domain.workoutsession.repository;

import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Timestamp;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    // 활성 세션(진행/일시정지) 중 가장 최근 1건. 상세는 findDetailByIdAndMemberId 로 따로 조회한다.
    Optional<WorkoutSession> findFirstByMemberIdAndStatusInOrderByIdDesc(Long memberId, Collection<SessionStatus> statuses);

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<SessionStatus> statuses);

    // 세션 변경은 행 잠금으로 직렬화한다 (동시 세트 추가/완료 시 번호 중복·완료 판정 경합 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WorkoutSession ws WHERE ws.id = :id")
    Optional<WorkoutSession> findByIdForUpdate(@Param("id") Long id);

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

    // 완료한 운동의 한국 날짜(YYYY-MM-DD) 목록. 드라이버의 시간 타입 매핑에 의존하지 않도록 문자열로 반환한다.
    @Query(value = "SELECT DISTINCT to_char(start_time AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') " +
            "FROM workout_session WHERE member_id = :memberId AND status = 'COMPLETED' AND start_time IS NOT NULL",
            nativeQuery = true)
    List<String> findCompletedWorkoutDatesKst(@Param("memberId") Long memberId);

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
            SELECT COALESCE(wse.body_part_name, wp.name) AS bodyPart, COUNT(*) AS count
            FROM workout_session ws
            JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            JOIN workout w ON wse.workout_id = w.id
            JOIN workout_part wp ON w.workout_part_id = wp.id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY COALESCE(wse.body_part_name, wp.name)
            ORDER BY count DESC
            """, nativeQuery = true)
    List<BodyPartStatProjection> findBodyPartStats(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT COALESCE(wse.body_part_name, wp.name)
            FROM workout_session ws
            JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            JOIN workout w ON wse.workout_id = w.id
            JOIN workout_part wp ON w.workout_part_id = wp.id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY COALESCE(wse.body_part_name, wp.name)
            ORDER BY COUNT(*) DESC
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
                COALESCE(ws.program_name, wp.name) AS programName,
                ws.start_time AS startTime,
                (EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) - COALESCE(ws.total_paused_seconds, 0)) AS durationSeconds,
                COUNT(CASE WHEN wss.completed = true THEN 1 END) AS completedSets,
                COUNT(wss.id) AS totalSets
            FROM workout_session ws
            LEFT JOIN workout_program wp ON ws.workout_program_id = wp.id
            LEFT JOIN workout_session_exercise wse ON ws.id = wse.workout_session_id
            LEFT JOIN workout_session_set wss ON wse.id = wss.workout_session_exercise_id
            WHERE ws.member_id = :memberId AND ws.status = 'COMPLETED'
            GROUP BY ws.id, ws.program_name, wp.name, ws.start_time, ws.end_time, ws.total_paused_seconds
            ORDER BY ws.start_time DESC
            LIMIT 3
            """, nativeQuery = true)
    List<RecentWorkoutProjection> findRecentCompleted(@Param("memberId") Long memberId);
}
