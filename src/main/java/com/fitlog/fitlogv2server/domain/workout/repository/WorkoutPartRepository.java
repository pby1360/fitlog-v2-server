package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutPartRepository extends JpaRepository<WorkoutPart, Long> {

    // 선택 가능한 부위 목록: 공용 + 본인 소유, 보관된 부위 제외
    @Query("SELECT p FROM WorkoutPart p WHERE (p.member IS NULL OR p.member.id = :memberId) AND p.archivedAt IS NULL")
    List<WorkoutPart> findAllVisible(@Param("memberId") Long memberId);

    Optional<WorkoutPart> findByName(String name);

    // 공용 부위(member IS NULL) 또는 본인 소유 부위만 조회.
    // 보관된 부위도 포함한다: 이미 그 부위를 담은 프로그램을 계속 수정할 수 있어야 한다 (신규 선택 목록에서만 제외).
    @Query("SELECT p FROM WorkoutPart p WHERE p.id = :id AND (p.member IS NULL OR p.member.id = :memberId)")
    Optional<WorkoutPart> findAccessibleById(@Param("id") Long id, @Param("memberId") Long memberId);

    // 같은 이름의 (공용 또는 본인) 부위가 있는지. 수정 시에는 자기 자신을 제외한다.
    @Query("SELECT COUNT(p) > 0 FROM WorkoutPart p " +
            "WHERE p.name = :name AND p.archivedAt IS NULL " +
            "AND (p.member IS NULL OR p.member.id = :memberId) " +
            "AND p.id <> :excludeId") // 신규 생성 시 excludeId = 0
    boolean existsVisibleName(@Param("memberId") Long memberId, @Param("name") String name,
                              @Param("excludeId") long excludeId);

    // 프로그램이나 (보관된 것 포함) 운동이 참조 중인지
    @Query(value = "SELECT EXISTS (SELECT 1 FROM workout_program_part WHERE workout_part_id = :id) " +
            "OR EXISTS (SELECT 1 FROM workout WHERE workout_part_id = :id)", nativeQuery = true)
    boolean isReferenced(@Param("id") Long id);
}
