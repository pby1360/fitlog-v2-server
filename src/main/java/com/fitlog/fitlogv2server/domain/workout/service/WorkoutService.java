package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutPartRepository;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 운동 부위·종목 마스터.
 * - 공용(member == null)은 누구도 수정/삭제할 수 없고, 개인 항목은 소유자만 수정/삭제할 수 있다.
 * - 이름은 공용 + 본인 항목 안에서만 겹치지 않으면 된다 (다른 회원의 개인 항목과는 무관).
 * - 프로그램이나 운동 기록이 참조 중인 항목은 삭제 대신 보관해 과거 기록을 보존한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutService {

    // 중복 검사에서 제외할 항목이 없을 때 (ID는 1부터 시작)
    private static final long NO_EXCLUDE = 0L;

    private final WorkoutRepository workoutRepository;
    private final WorkoutPartRepository workoutPartRepository;

    @Transactional(readOnly = true)
    public List<WorkoutPartDto> getWorkoutParts(Long memberId) {
        List<WorkoutPart> workoutParts = workoutPartRepository.findAllVisible(memberId);
        return workoutParts.stream()
                .map(WorkoutPartDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> getWorkouts(Long memberId) {
        List<Workout> workouts = workoutRepository.findAllVisible(memberId);
        return workouts.stream()
                .map(WorkoutDto::new)
                .collect(Collectors.toList());
    }

    public void addWorkoutPart(WorkoutPartDto.Request request, Member member) {
        String name = request.getName().trim();
        assertPartNameAvailable(member.getId(), name, NO_EXCLUDE);
        workoutPartRepository.save(WorkoutPart.builder()
                .name(name)
                .member(member)
                .build());
    }

    public void updateWorkoutPart(Long workoutPartId, WorkoutPartDto.Request request, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(workoutPartId, member.getId());
        if (!isOwnedBy(workoutPart.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        String name = request.getName().trim();
        assertPartNameAvailable(member.getId(), name, workoutPart.getId());
        workoutPart.updateName(name);
    }

    /**
     * 개인 부위 삭제: 부위 안의 본인 운동을 먼저 삭제(또는 보관)하고,
     * 부위가 여전히 참조 중이면(프로그램, 보관된 운동 등) 보관, 아니면 삭제한다.
     */
    public void deleteWorkoutPart(Long workoutPartId, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(workoutPartId, member.getId());
        if (!isOwnedBy(workoutPart.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        // 다른 회원의 운동은 건드리지 않는다
        for (Workout workout : workoutRepository.findAllByWorkoutPartIdAndMemberId(workoutPartId, member.getId())) {
            deleteOrArchive(workout);
        }
        workoutRepository.flush();

        if (workoutPartRepository.isReferenced(workoutPart.getId())) {
            workoutPart.archive();
        } else {
            workoutPartRepository.delete(workoutPart);
        }
    }

    public void addWorkout(WorkoutDto.Request request, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(request.getWorkoutPartId(), member.getId());
        String name = request.getName().trim();
        assertWorkoutNameAvailable(member.getId(), workoutPart.getId(), name, NO_EXCLUDE);
        workoutRepository.save(Workout.builder()
                .name(name)
                .workoutPart(workoutPart)
                .member(member)
                .build());
    }

    public void updateWorkout(Long workoutId, WorkoutDto.Request request, Member member) {
        Workout workout = findAccessibleWorkout(workoutId, member.getId());
        if (!isOwnedBy(workout.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        WorkoutPart workoutPart = findAccessibleWorkoutPart(request.getWorkoutPartId(), member.getId());
        String name = request.getName().trim();
        assertWorkoutNameAvailable(member.getId(), workoutPart.getId(), name, workout.getId());
        // 이름/부위를 바꿔도 과거 운동 기록은 세션에 저장된 이름 스냅샷으로 표시된다
        workout.update(name, workoutPart);
    }

    public void deleteWorkout(Long workoutId, Member member) {
        Workout workout = findAccessibleWorkout(workoutId, member.getId());
        if (!isOwnedBy(workout.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        deleteOrArchive(workout);
    }

    @Transactional(readOnly = true)
    public WorkoutPart findWorkoutPartByName(String name) {
        return workoutPartRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
    }

    @Transactional(readOnly = true)
    public Workout findWorkoutByName(String name) {
        return workoutRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
    }

    /**
     * 공용 부위 또는 본인 소유 부위만 반환한다. 다른 회원의 부위는 존재하지 않는 것으로 취급한다.
     */
    @Transactional(readOnly = true)
    public WorkoutPart findAccessibleWorkoutPart(Long id, Long memberId) {
        return workoutPartRepository.findAccessibleById(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동 부위입니다."));
    }

    /**
     * 공용 운동 또는 본인 소유 운동만 반환한다. 다른 회원의 운동은 존재하지 않는 것으로 취급한다.
     */
    @Transactional(readOnly = true)
    public Workout findAccessibleWorkout(Long id, Long memberId) {
        return workoutRepository.findAccessibleById(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 운동입니다."));
    }

    // 참조 중이면 보관(신규 선택에서만 제외, 과거 기록 유지), 아니면 삭제
    private void deleteOrArchive(Workout workout) {
        if (workoutRepository.isReferenced(workout.getId())) {
            workout.archive();
        } else {
            workoutRepository.delete(workout);
        }
    }

    private void assertPartNameAvailable(Long memberId, String name, long excludeId) {
        if (workoutPartRepository.existsVisibleName(memberId, name, excludeId)) {
            throw new ConflictException("이미 같은 이름의 운동 부위가 있습니다.");
        }
    }

    private void assertWorkoutNameAvailable(Long memberId, Long partId, String name, long excludeId) {
        if (workoutRepository.existsVisibleName(memberId, partId, name, excludeId)) {
            throw new ConflictException("같은 부위에 이미 같은 이름의 운동이 있습니다.");
        }
    }

    // 공용 마스터(owner == null)는 어떤 회원도 수정/삭제할 수 없다
    private boolean isOwnedBy(Member owner, Member member) {
        return owner != null && owner.getId().equals(member.getId());
    }
}
