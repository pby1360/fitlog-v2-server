package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutPartRepository extends JpaRepository<WorkoutPart, Long> {
    List<WorkoutPart> findAllByMemberIdOrMemberIsNull(Long memberId);
    Optional<WorkoutPart> findByName(String name);

    // 공용 부위(member IS NULL) 또는 본인 소유 부위만 조회
    @Query("SELECT p FROM WorkoutPart p WHERE p.id = :id AND (p.member IS NULL OR p.member.id = :memberId)")
    Optional<WorkoutPart> findAccessibleById(@Param("id") Long id, @Param("memberId") Long memberId);
}
