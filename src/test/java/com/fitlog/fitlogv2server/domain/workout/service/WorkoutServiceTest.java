package com.fitlog.fitlogv2server.domain.workout.service;

import com.fitlog.fitlogv2server.global.exception.NotFoundException;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutPartRepository;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;
    @Mock
    private WorkoutPartRepository workoutPartRepository;

    @InjectMocks
    private WorkoutService workoutService;

    private static final Long MEMBER_ID = 10L;
    private static final Long OTHER_MEMBER_ID = 20L;

    @Test
    void addWorkout_rejectsPartOwnedByAnotherMember() {
        Member member = buildMember(MEMBER_ID);
        // 다른 회원의 개인 부위는 접근 가능 조회에서 결과가 없다
        given(workoutPartRepository.findAccessibleById(5L, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.addWorkout(buildWorkoutRequest("인클라인 머신", 5L), member))
                .isInstanceOf(NotFoundException.class);
        verify(workoutRepository, never()).save(any(Workout.class));
    }

    @Test
    void updateWorkout_rejectsPublicMasterWithForbidden() {
        Member member = buildMember(MEMBER_ID);
        Workout publicWorkout = Workout.builder().name("벤치프레스").workoutPart(buildPart(1L, null)).member(null).build();
        given(workoutRepository.findAccessibleById(3L, MEMBER_ID)).willReturn(Optional.of(publicWorkout));

        assertThatThrownBy(() -> workoutService.updateWorkout(3L, buildWorkoutRequest("변경", 1L), member))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteWorkout_rejectsPublicMasterWithForbidden() {
        Member member = buildMember(MEMBER_ID);
        Workout publicWorkout = Workout.builder().name("벤치프레스").workoutPart(buildPart(1L, null)).member(null).build();
        given(workoutRepository.findAccessibleById(3L, MEMBER_ID)).willReturn(Optional.of(publicWorkout));

        assertThatThrownBy(() -> workoutService.deleteWorkout(3L, member))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(workoutRepository, never()).delete(any(Workout.class));
    }

    @Test
    void deleteWorkoutPart_rejectsPartOwnedByAnotherMember() {
        Member member = buildMember(MEMBER_ID);
        given(workoutPartRepository.findAccessibleById(5L, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.deleteWorkoutPart(5L, member))
                .isInstanceOf(NotFoundException.class);
        verify(workoutRepository, never()).findAllByWorkoutPartIdAndMemberId(anyLong(), anyLong());
    }


    @Test
    void findAccessibleWorkout_hidesOtherMembersWorkout() {
        given(workoutRepository.findAccessibleById(9L, OTHER_MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.findAccessibleWorkout(9L, OTHER_MEMBER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    private Member buildMember(Long id) {
        Member member = Member.builder()
                .email("user" + id + "@example.com")
                .nickname("user" + id)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private WorkoutPart buildPart(Long id, Member owner) {
        WorkoutPart part = WorkoutPart.builder().name("가슴").member(owner).build();
        ReflectionTestUtils.setField(part, "id", id);
        return part;
    }

    private WorkoutDto.Request buildWorkoutRequest(String name, Long workoutPartId) {
        WorkoutDto.Request request = new WorkoutDto.Request();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "workoutPartId", workoutPartId);
        return request;
    }
}
