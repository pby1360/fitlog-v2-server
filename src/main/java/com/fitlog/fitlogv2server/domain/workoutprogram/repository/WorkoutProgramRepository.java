package com.fitlog.fitlogv2server.domain.workoutprogram.repository;

import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {
    // 소프트삭제되지 않은 프로그램만 조회 (목록에서 삭제건 숨김)
    List<WorkoutProgram> findAllByMemberIdAndDeletedAtIsNull(Long memberId);
}
