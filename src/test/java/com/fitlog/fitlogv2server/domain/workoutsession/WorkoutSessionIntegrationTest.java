package com.fitlog.fitlogv2server.domain.workoutsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.domain.dashboard.dto.DashboardStatsDto;
import com.fitlog.fitlogv2server.domain.dashboard.service.DashboardService;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.SessionStatus;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSession;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionExercise;
import com.fitlog.fitlogv2server.domain.workoutsession.entity.WorkoutSessionSet;
import com.fitlog.fitlogv2server.domain.workoutsession.facade.WorkoutSessionFacade;
import com.fitlog.fitlogv2server.domain.workoutsession.repository.WorkoutSessionRepository;
import com.fitlog.fitlogv2server.domain.workoutsession.service.WorkoutSessionService;
import com.fitlog.fitlogv2server.global.common.AppTimeZone;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 PostgreSQL + 마이그레이션 제약 위에서 세션 규칙을 검증한다 (Mock으로는 잡히지 않는 영속성/제약 문제).
 */
class WorkoutSessionIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkoutSessionService workoutSessionService;
    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;
    @Autowired
    private WorkoutSessionFacade workoutSessionFacade;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void secondActiveSession_isRejected() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(1), 1));

        assertThatThrownBy(() -> workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(1), 1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void databaseAlsoRejectsSecondActiveSession() {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        workoutSessionRepository.save(WorkoutSession.builder().member(member).workoutProgram(program)
                .startTime(ZonedDateTime.now(AppTimeZone.KST)).status(SessionStatus.IN_PROGRESS).build());

        // 서비스 검사를 우회한 동시 요청 상황: 부분 유니크 인덱스가 막는다
        assertThatThrownBy(() -> workoutSessionRepository.save(WorkoutSession.builder().member(member).workoutProgram(program)
                .startTime(ZonedDateTime.now(AppTimeZone.KST)).status(SessionStatus.PAUSED).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void latestSession_isReturnedWithDetails() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        WorkoutSession started = workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(2), 3));

        WorkoutSessionDto.Response latest = workoutSessionFacade.getLatestInProgressSession(member.getId()).orElseThrow();

        assertThat(latest.getId()).isEqualTo(started.getId());
        assertThat(latest.getExercises()).hasSize(2);
    }

    @Test
    void removedExercise_staysRemovedAfterReload() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        WorkoutSession session = workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(2), 2));
        Long removeId = session.getWorkoutSessionExercises().stream()
                .min(Comparator.comparing(WorkoutSessionExercise::getOrder)).orElseThrow().getId();

        workoutSessionService.removeExercise(member.getId(), session.getId(), removeId);

        // 새 트랜잭션에서 다시 조회해도 삭제되어 있어야 한다 (응답에서만 사라지던 문제)
        WorkoutSession reloaded = workoutSessionService.getSessionDetail(member.getId(), session.getId());
        assertThat(reloaded.getWorkoutSessionExercises()).hasSize(1);
        assertThat(reloaded.getWorkoutSessionExercises().iterator().next().getOrder()).isEqualTo(1);
        Integer orphanSets = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout_session_set WHERE workout_session_exercise_id = ?", Integer.class, removeId);
        assertThat(orphanSets).isZero();
    }

    @Test
    void endedSession_cannotBeModified() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        WorkoutSession session = workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(1), 1));
        workoutSessionService.endSession(member.getId(), session.getId(), endRequest("CANCELLED"));

        Long exerciseId = session.getWorkoutSessionExercises().iterator().next().getId();
        WorkoutSessionSet set = session.getWorkoutSessionExercises().iterator().next().getWorkoutSessionSets().iterator().next();

        assertThatThrownBy(() -> workoutSessionService.completeSet(member.getId(), session.getId(),
                completeRequest(exerciseId, set.getId())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> workoutSessionService.removeExercise(member.getId(), session.getId(), exerciseId))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> workoutSessionService.endSession(member.getId(), session.getId(), endRequest("COMPLETED")))
                .isInstanceOf(ConflictException.class);

        WorkoutSession reloaded = workoutSessionService.getSessionDetail(member.getId(), session.getId());
        assertThat(reloaded.getStatus()).isEqualTo(SessionStatus.CANCELLED);
    }

    @Test
    void endSession_ignoresClientEndTime() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        WorkoutSession session = workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(1), 1));

        WorkoutSessionDto.EndRequest request = objectMapper.findAndRegisterModules().readValue(
                "{\"status\":\"COMPLETED\",\"endTime\":\"2000-01-01T00:00:00Z\"}", WorkoutSessionDto.EndRequest.class);
        WorkoutSession ended = workoutSessionService.endSession(member.getId(), session.getId(), request);

        assertThat(ended.getEndTime()).isAfterOrEqualTo(ended.getStartTime());
        assertThat(ended.getEndTime().getYear()).isGreaterThan(2000);
    }

    @Test
    void completingAllSets_completesSession_andDashboardShowsPercent() throws Exception {
        Member member = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(member);
        WorkoutSession session = workoutSessionService.startSession(member, startRequest(program, fixtures.publicWorkouts(1), 2));
        WorkoutSessionExercise exercise = session.getWorkoutSessionExercises().iterator().next();
        List<WorkoutSessionSet> sets = exercise.getWorkoutSessionSets().stream()
                .sorted(Comparator.comparing(WorkoutSessionSet::getSetNumber)).toList();

        workoutSessionService.completeSet(member.getId(), session.getId(), completeRequest(exercise.getId(), sets.get(0).getId()));
        WorkoutSession afterLast = workoutSessionService.completeSet(member.getId(), session.getId(),
                completeRequest(exercise.getId(), sets.get(1).getId()));

        assertThat(afterLast.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        DashboardStatsDto stats = dashboardService.getStats(member.getId());
        assertThat(stats.getAverageCompletionRate()).isEqualTo(100.0);
        assertThat(stats.getTotalWorkouts()).isEqualTo(1);
    }

    // --- 요청 생성 헬퍼 (요청 DTO에는 setter가 없으므로 JSON으로 만든다) ---

    private WorkoutSessionDto.StartRequest startRequest(WorkoutProgram program, List<Workout> workouts, int setsPerExercise) throws Exception {
        StringBuilder exercises = new StringBuilder();
        for (int i = 0; i < workouts.size(); i++) {
            if (i > 0) exercises.append(',');
            StringBuilder sets = new StringBuilder();
            for (int s = 0; s < setsPerExercise; s++) {
                if (s > 0) sets.append(',');
                sets.append("{\"setNumber\":").append(s + 1).append(",\"weight\":40,\"reps\":10,\"restTime\":60}");
            }
            exercises.append("{\"workoutId\":").append(workouts.get(i).getId())
                    .append(",\"order\":").append(i + 1)
                    .append(",\"sets\":[").append(sets).append("]}");
        }
        return objectMapper.readValue("{\"workoutProgramId\":" + program.getId() + ",\"customExercises\":[" + exercises + "]}",
                WorkoutSessionDto.StartRequest.class);
    }

    private WorkoutSessionDto.EndRequest endRequest(String status) throws Exception {
        return objectMapper.readValue("{\"status\":\"" + status + "\"}", WorkoutSessionDto.EndRequest.class);
    }

    private WorkoutSessionDto.CompleteSetRequest completeRequest(Long exerciseId, Long setId) throws Exception {
        return objectMapper.readValue("{\"workoutSessionExerciseId\":" + exerciseId + ",\"workoutSessionSetId\":" + setId
                + ",\"actualWeight\":40,\"actualReps\":10}", WorkoutSessionDto.CompleteSetRequest.class);
    }
}
