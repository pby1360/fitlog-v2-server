package com.fitlog.fitlogv2server.domain.workout.repository;

import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findAllByMemberIdOrMemberIsNull(Long memberId);
    Optional<Workout> findByName(String name);
    void deleteAllByWorkoutPartIdAndMemberId(Long workoutPartId, Long memberId);

    // 공용 운동(member IS NULL) 또는 본인 소유 운동만 조회
    @Query("SELECT w FROM Workout w WHERE w.id = :id AND (w.member IS NULL OR w.member.id = :memberId)")
    Optional<Workout> findAccessibleById(@Param("id") Long id, @Param("memberId") Long memberId);
}
