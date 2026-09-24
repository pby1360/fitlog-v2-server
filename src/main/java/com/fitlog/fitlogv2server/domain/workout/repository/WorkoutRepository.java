package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    // 선택 가능한 운동 목록: 공용 + 본인 소유, 보관된 운동 제외
    @Query("SELECT w FROM Workout w WHERE (w.member IS NULL OR w.member.id = :memberId) AND w.archivedAt IS NULL")
    List<Workout> findAllVisible(@Param("memberId") Long memberId);

    Optional<Workout> findByName(String name);

    List<Workout> findAllByWorkoutPartIdAndMemberId(Long workoutPartId, Long memberId);

    // 공용 운동(member IS NULL) 또는 본인 소유 운동만 조회.
    // 보관된 운동도 포함한다: 이미 그 운동을 담은 프로그램으로 운동을 시작/수정할 수 있어야 한다 (신규 선택 목록에서만 제외).
    @Query("SELECT w FROM Workout w WHERE w.id = :id AND (w.member IS NULL OR w.member.id = :memberId)")
    Optional<Workout> findAccessibleById(@Param("id") Long id, @Param("memberId") Long memberId);

    // 같은 부위 안에 같은 이름의 (공용 또는 본인) 운동이 있는지. 수정 시에는 자기 자신을 제외한다.
    @Query("SELECT COUNT(w) > 0 FROM Workout w " +
            "WHERE w.workoutPart.id = :partId AND w.name = :name AND w.archivedAt IS NULL " +
            "AND (w.member IS NULL OR w.member.id = :memberId) " +
            "AND w.id <> :excludeId") // 신규 생성 시 excludeId = 0
    boolean existsVisibleName(@Param("memberId") Long memberId, @Param("partId") Long partId,
                              @Param("name") String name, @Param("excludeId") long excludeId);

    // 프로그램이나 운동 기록에서 참조 중인지 (참조 중이면 물리 삭제 대신 보관)
    @Query(value = "SELECT EXISTS (SELECT 1 FROM workout_program_exercise WHERE workout_id = :id) " +
            "OR EXISTS (SELECT 1 FROM workout_session_exercise WHERE workout_id = :id)", nativeQuery = true)
    boolean isReferenced(@Param("id") Long id);
}
