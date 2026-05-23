package com.fitlog.fitlogv2server.domain.workoutsession.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.entity.WorkoutPart;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import com.fitlog.fitlogv2server.domain.workoutprogram.repository.WorkoutProgramRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionExerciseRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;
    @Mock
    private WorkoutProgramRepository workoutProgramRepository;
    @Mock
    private WorkoutSessionExerciseRepository workoutSessionExerciseRepository;
    @Mock
    private WorkoutSessionSetRepository workoutSessionSetRepository;
    @Mock
    private WorkoutRepository workoutRepository;

    @InjectMocks
    private WorkoutSessionService workoutSessionService;

    private static final Long MEMBER_ID = 10L;
    private static final Long SESSION_ID = 1L;
    private static final Long EXERCISE_ID = 100L;

    @Test
    void addSet_appendsSetToEndWithSequentialSetNumber() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1, 2);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(workoutSessionSetRepository.save(any(WorkoutSessionSet.class))).willAnswer(inv -> inv.getArgument(0));

        WorkoutSession result = workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, 60, "메모"));

        ArgumentCaptor<WorkoutSessionSet> captor = ArgumentCaptor.forClass(WorkoutSessionSet.class);
        verify(workoutSessionSetRepository).save(captor.capture());
        WorkoutSessionSet saved = captor.getValue();
        assertThat(saved.getSetNumber()).isEqualTo(3);
        assertThat(saved.getWeight()).isEqualTo(60.0);
        assertThat(saved.getReps()).isEqualTo(10);
        assertThat(saved.getRestTime()).isEqualTo(60);
        assertThat(saved.getMemo()).isEqualTo("메모");
        assertThat(saved.getCompleted()).isFalse();
        assertThat(saved.getActualWeight()).isNull();
        assertThat(saved.getActualReps()).isNull();
        assertThat(saved.getActualMemo()).isNull();
        assertThat(saved.getCompletedAt()).isNull();

        WorkoutSessionExercise exercise = result.getWorkoutSessionExercises().iterator().next();
        assertThat(exercise.getWorkoutSessionSets()).hasSize(3);
        assertThat(exercise.getWorkoutSessionSets())
                .anyMatch(s -> s.getSetNumber() == 3 && Boolean.FALSE.equals(s.getCompleted()));
    }

    @Test
    void addSet_startsAtSetNumberOneWhenNoExistingSets() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(workoutSessionSetRepository.save(any(WorkoutSessionSet.class))).willAnswer(inv -> inv.getArgument(0));

        workoutSessionService.addSet(MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(null, 8, 90, null));

        ArgumentCaptor<WorkoutSessionSet> captor = ArgumentCaptor.forClass(WorkoutSessionSet.class);
        verify(workoutSessionSetRepository).save(captor.capture());
        assertThat(captor.getValue().getSetNumber()).isEqualTo(1);
        assertThat(captor.getValue().getWeight()).isNull();
    }

    @Test
    void addSet_rejectsWhenSessionCompletedOrCancelled() {
        for (SessionStatus status : List.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED)) {
            WorkoutSession session = buildSession(status, 1);
            given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.addSet(
                    MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, 60, null)))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
        verify(workoutSessionSetRepository, never()).save(any());
    }

    @Test
    void addSet_allowedWhenSessionPaused() {
        WorkoutSession session = buildSession(SessionStatus.PAUSED, 1, 2);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(workoutSessionSetRepository.save(any(WorkoutSessionSet.class))).willAnswer(inv -> inv.getArgument(0));

        WorkoutSession result = workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, 60, null));

        ArgumentCaptor<WorkoutSessionSet> captor = ArgumentCaptor.forClass(WorkoutSessionSet.class);
        verify(workoutSessionSetRepository).save(captor.capture());
        assertThat(captor.getValue().getSetNumber()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(SessionStatus.PAUSED);
    }

    @Test
    void addSet_rejectsWhenSessionBelongsToAnotherMember() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addSet(
                999L, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, 60, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(workoutSessionSetRepository, never()).save(any());
    }

    @Test
    void addSet_rejectsWhenSessionNotFound() {
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, 60, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addSet_rejectsWhenExerciseNotInSession() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, 999L, buildRequest(60.0, 10, 60, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(workoutSessionSetRepository, never()).save(any());
    }

    @Test
    void addSet_rejectsWhenRepsMissing() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, null, 60, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addSet_rejectsWhenRestTimeMissing() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addSet(
                MEMBER_ID, SESSION_ID, EXERCISE_ID, buildRequest(60.0, 10, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addExercise_appendsExerciseWithRequestedSets() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(workoutRepository.findById(12L)).willReturn(Optional.of(buildWorkout()));

        WorkoutSessionDto.AddExerciseRequest request = buildAddExerciseRequest(12L, 2,
                List.of(buildAddSetRequest(40.0, 10, 60, "첫 세트"), buildAddSetRequest(45.0, 8, 90, null)));

        WorkoutSession result = workoutSessionService.addExercise(MEMBER_ID, SESSION_ID, request);

        WorkoutSessionExercise added = result.getWorkoutSessionExercises().stream()
                .filter(e -> e.getOrder() == 2)
                .findFirst()
                .orElseThrow();
        assertThat(added.getWorkout().getId()).isEqualTo(12L);
        assertThat(added.getSkipped()).isFalse();
        assertThat(added.getStartedAt()).isNull();
        assertThat(added.getWorkoutSessionSets()).hasSize(2);
        assertThat(added.getWorkoutSessionSets())
                .extracting(WorkoutSessionSet::getSetNumber)
                .containsExactlyInAnyOrder(1, 2);
        assertThat(added.getWorkoutSessionSets())
                .allMatch(s -> Boolean.FALSE.equals(s.getCompleted())
                        && s.getActualWeight() == null
                        && s.getActualReps() == null
                        && s.getActualMemo() == null
                        && s.getCompletedAt() == null);
    }

    @Test
    void addExercise_rejectsWhenSetsEmpty() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addExercise(
                MEMBER_ID, SESSION_ID, buildAddExerciseRequest(12L, 2, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addExercise_rejectsWhenSessionBelongsToAnotherMember() {
        WorkoutSession session = buildSession(SessionStatus.IN_PROGRESS, 1);
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.addExercise(
                999L, SESSION_ID, buildAddExerciseRequest(12L, 2, List.of(buildAddSetRequest(40.0, 10, 60, null)))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void addExercise_rejectsWhenSessionNotFound() {
        given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.addExercise(
                MEMBER_ID, SESSION_ID, buildAddExerciseRequest(12L, 2, List.of(buildAddSetRequest(40.0, 10, 60, null)))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addExercise_rejectsWhenSessionCompletedOrCancelled() {
        for (SessionStatus status : List.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED)) {
            WorkoutSession session = buildSession(status, 1);
            given(workoutSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.addExercise(
                    MEMBER_ID, SESSION_ID, buildAddExerciseRequest(12L, 2, List.of(buildAddSetRequest(40.0, 10, 60, null)))))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    private WorkoutSession buildSession(SessionStatus status, int... existingSetNumbers) {
        Member member = Member.builder()
                .email("user@example.com")
                .nickname("user")
                .build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);

        WorkoutSession session = WorkoutSession.builder()
                .member(member)
                .startTime(ZonedDateTime.now(ZoneOffset.UTC))
                .status(status)
                .build();
        ReflectionTestUtils.setField(session, "id", SESSION_ID);

        WorkoutSessionExercise exercise = WorkoutSessionExercise.builder()
                .workoutSession(session)
                .order(1)
                .build();
        ReflectionTestUtils.setField(exercise, "id", EXERCISE_ID);

        long setId = 1000L;
        for (int setNumber : existingSetNumbers) {
            WorkoutSessionSet set = WorkoutSessionSet.builder()
                    .workoutSessionExercise(exercise)
                    .setNumber(setNumber)
                    .weight(50.0)
                    .reps(10)
                    .restTime(60)
                    .completed(true)
                    .build();
            ReflectionTestUtils.setField(set, "id", setId++);
            exercise.addWorkoutSessionSet(set);
        }

        session.addWorkoutSessionExercise(exercise);
        return session;
    }

    private WorkoutSessionDto.CreateSetRequest buildRequest(Double weight, Integer reps, Integer restTime, String memo) {
        WorkoutSessionDto.CreateSetRequest request = new WorkoutSessionDto.CreateSetRequest();
        ReflectionTestUtils.setField(request, "weight", weight);
        ReflectionTestUtils.setField(request, "reps", reps);
        ReflectionTestUtils.setField(request, "restTime", restTime);
        ReflectionTestUtils.setField(request, "memo", memo);
        return request;
    }

    private WorkoutSessionDto.AddExerciseRequest buildAddExerciseRequest(Long workoutId, Integer order, List<WorkoutSessionDto.AddSetRequest> sets) {
        WorkoutSessionDto.AddExerciseRequest request = new WorkoutSessionDto.AddExerciseRequest();
        ReflectionTestUtils.setField(request, "workoutId", workoutId);
        ReflectionTestUtils.setField(request, "order", order);
        ReflectionTestUtils.setField(request, "sets", sets);
        return request;
    }

    private WorkoutSessionDto.AddSetRequest buildAddSetRequest(Double weight, Integer reps, Integer restTime, String memo) {
        WorkoutSessionDto.AddSetRequest request = new WorkoutSessionDto.AddSetRequest();
        ReflectionTestUtils.setField(request, "setNumber", 1);
        ReflectionTestUtils.setField(request, "weight", weight);
        ReflectionTestUtils.setField(request, "reps", reps);
        ReflectionTestUtils.setField(request, "restTime", restTime);
        ReflectionTestUtils.setField(request, "memo", memo);
        return request;
    }

    private Workout buildWorkout() {
        WorkoutPart part = WorkoutPart.builder().name("가슴").build();
        ReflectionTestUtils.setField(part, "id", 5L);
        Workout workout = Workout.builder().name("벤치프레스").workoutPart(part).build();
        ReflectionTestUtils.setField(workout, "id", 12L);
        return workout;
    }
}
