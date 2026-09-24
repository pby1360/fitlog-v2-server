package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutPartRepository workoutPartRepository;

    @Transactional(readOnly = true)
    public List<WorkoutPartDto> getWorkoutParts(Long memberId) {
        List<WorkoutPart> workoutParts = workoutPartRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workoutParts.stream()
                .map(WorkoutPartDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> getWorkouts(Long memberId) {
        List<Workout> workouts = workoutRepository.findAllByMemberIdOrMemberIsNull(memberId);
        return workouts.stream()
                .map(WorkoutDto::new)
                .collect(Collectors.toList());
    }

    public void addWorkoutPart(WorkoutPartDto.Request request, Member member) {
        workoutPartRepository.save(WorkoutPart.builder()
                .name(request.getName())
                .member(member)
                .build());
    }

    public void updateWorkoutPart(Long workoutPartId, WorkoutPartDto.Request request, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(workoutPartId, member.getId());
        if (!isOwnedBy(workoutPart.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        workoutPart.updateName(request.getName());
    }

    public void deleteWorkoutPart(Long workoutPartId, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(workoutPartId, member.getId());
        if (!isOwnedBy(workoutPart.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        // 본인 소유 운동만 삭제한다 (다른 회원의 운동까지 지우지 않도록)
        workoutRepository.deleteAllByWorkoutPartIdAndMemberId(workoutPartId, member.getId());
        workoutPartRepository.delete(workoutPart);
    }

    public void addWorkout(WorkoutDto.Request request, Member member) {
        WorkoutPart workoutPart = findAccessibleWorkoutPart(request.getWorkoutPartId(), member.getId());
        workoutRepository.save(Workout.builder()
                .name(request.getName())
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
        workout.update(request.getName(), workoutPart);
    }

    public void deleteWorkout(Long workoutId, Member member) {
        Workout workout = findAccessibleWorkout(workoutId, member.getId());
        if (!isOwnedBy(workout.getMember(), member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        workoutRepository.delete(workout);
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

    // 공용 마스터(owner == null)는 어떤 회원도 수정/삭제할 수 없다
    private boolean isOwnedBy(Member owner, Member member) {
        return owner != null && owner.getId().equals(member.getId());
    }
}
