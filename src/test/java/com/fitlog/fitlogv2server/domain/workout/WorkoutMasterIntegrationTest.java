package com.fitlog.fitlogv2server.domain.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.service.WorkoutProgramService;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.facade.WorkoutSessionFacade;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutMasterIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkoutService workoutService;
    @Autowired
    private WorkoutProgramService workoutProgramService;
    @Autowired
    private WorkoutSessionFacade workoutSessionFacade;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void differentMembers_canUseSamePersonalName() throws Exception {
        Member a = fixtures.member();
        Member b = fixtures.member();
        Long chest = publicPartId("가슴");
        String name = "인클라인 머신 " + UUID.randomUUID();

        workoutService.addWorkout(workoutRequest(name, chest), a);
        workoutService.addWorkout(workoutRequest(name, chest), b);

        assertThat(workoutService.getWorkouts(b.getId())).extracting(WorkoutDto::getName).contains(name);
        // A의 개인 운동은 B의 목록에 보이지 않는다
        assertThat(workoutService.getWorkouts(b.getId()).stream().filter(w -> w.getName().equals(name))).hasSize(1);
    }

    @Test
    void duplicateOwnOrPublicName_isConflict() throws Exception {
        Member member = fixtures.member();
        Long chest = publicPartId("가슴");
        workoutService.addWorkout(workoutRequest("나만의 운동", chest), member);

        assertThatThrownBy(() -> workoutService.addWorkout(workoutRequest("나만의 운동", chest), member))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> workoutService.addWorkout(workoutRequest("벤치프레스", chest), member))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> workoutService.addWorkoutPart(partRequest("가슴"), member))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void personalPartNames_areScopedPerMember() throws Exception {
        Member a = fixtures.member();
        Member b = fixtures.member();

        workoutService.addWorkoutPart(partRequest("전완"), a);
        workoutService.addWorkoutPart(partRequest("전완"), b);

        assertThat(workoutService.getWorkoutParts(a.getId()).stream().filter(p -> p.getName().equals("전완"))).hasSize(1);
    }

    @Test
    void listMarksOnlyOwnItemsEditable() throws Exception {
        Member member = fixtures.member();
        workoutService.addWorkout(workoutRequest("내 운동", publicPartId("등")), member);

        List<WorkoutDto> workouts = workoutService.getWorkouts(member.getId());

        assertThat(workouts).filteredOn(WorkoutDto::isEditable).extracting(WorkoutDto::getName).containsExactly("내 운동");
        assertThat(workoutService.getWorkoutParts(member.getId())).noneMatch(WorkoutPartDto::isEditable);
    }

    @Test
    void referencedWorkout_isArchived_andHistoryKeepsOriginalName() throws Exception {
        Member member = fixtures.member();
        Long chest = publicPartId("가슴");
        workoutService.addWorkout(workoutRequest("케이블 크로스오버", chest), member);
        Long workoutId = ownWorkoutId(member, "케이블 크로스오버");

        WorkoutSessionDto.Response session = workoutSessionFacade.startSession(member.getId(), startRequest(fixtures.emptyProgram(member).getId(), workoutId));
        workoutService.updateWorkout(workoutId, workoutRequest("이름 바꾼 운동", chest), member);
        workoutService.deleteWorkout(workoutId, member);

        // 기록은 생성 당시 이름으로 남는다
        WorkoutSessionDto.Response detail = workoutSessionFacade.getSessionDetail(member.getId(), session.getId());
        assertThat(detail.getExercises().get(0).getWorkoutName()).isEqualTo("케이블 크로스오버");
        // 참조 중이라 물리 삭제 대신 보관: 목록/신규 선택에서는 빠지고, 같은 이름으로 다시 만들 수 있다
        assertThat(jdbcTemplate.queryForObject("SELECT archived_at IS NOT NULL FROM workout WHERE id = ?", Boolean.class, workoutId)).isTrue();
        assertThat(workoutService.getWorkouts(member.getId())).extracting(WorkoutDto::getName).doesNotContain("이름 바꾼 운동");
        workoutService.addWorkout(workoutRequest("이름 바꾼 운동", chest), member);
    }

    @Test
    void unreferencedWorkout_isDeleted() throws Exception {
        Member member = fixtures.member();
        workoutService.addWorkout(workoutRequest("안 쓴 운동", publicPartId("하체")), member);
        Long workoutId = ownWorkoutId(member, "안 쓴 운동");

        workoutService.deleteWorkout(workoutId, member);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workout WHERE id = ?", Integer.class, workoutId)).isZero();
    }

    @Test
    void deletingPartUsedByProgram_archivesPartAndKeepsProgramReadable() throws Exception {
        Member member = fixtures.member();
        workoutService.addWorkoutPart(partRequest("코어"), member);
        Long partId = workoutService.getWorkoutParts(member.getId()).stream()
                .filter(p -> p.getName().equals("코어")).findFirst().orElseThrow().getId();
        workoutService.addWorkout(workoutRequest("플랭크 변형", partId), member);
        Long workoutId = ownWorkoutId(member, "플랭크 변형");
        workoutProgramService.createWorkoutProgram(programRequest(partId, workoutId), member);

        workoutService.deleteWorkoutPart(partId, member);

        assertThat(workoutService.getWorkoutParts(member.getId())).extracting(WorkoutPartDto::getName).doesNotContain("코어");
        assertThat(workoutProgramService.findAllPrograms(member.getId()).get(0).parts().get(0).workoutPartName()).isEqualTo("코어");
    }

    @Test
    void programParts_areReturnedInSavedOrder() throws Exception {
        Member member = fixtures.member();
        Long back = publicPartId("등");
        Long chest = publicPartId("가슴");
        Long legs = publicPartId("하체");
        String json = "{\"name\":\"순서 테스트\",\"description\":\"\",\"parts\":["
                + partJson(legs, publicWorkoutId(legs)) + "," + partJson(chest, publicWorkoutId(chest)) + "," + partJson(back, publicWorkoutId(back)) + "]}";
        workoutProgramService.createWorkoutProgram(objectMapper.readValue(json, WorkoutProgramDto.Request.class), member);

        List<WorkoutProgramDto.Response.ProgramPartDto> parts = workoutProgramService.findAllPrograms(member.getId()).get(0).parts();

        assertThat(parts).extracting(WorkoutProgramDto.Response.ProgramPartDto::workoutPartId).containsExactly(legs, chest, back);
    }

    // --- helpers ---

    private Long publicPartId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM workout_part WHERE name = ? AND member_id IS NULL", Long.class, name);
    }

    private Long publicWorkoutId(Long partId) {
        return jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM workout WHERE workout_part_id = ? AND member_id IS NULL", Long.class, partId);
    }

    private Long ownWorkoutId(Member member, String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM workout WHERE member_id = ? AND name = ? AND archived_at IS NULL", Long.class, member.getId(), name);
    }

    private WorkoutDto.Request workoutRequest(String name, Long partId) throws Exception {
        return objectMapper.readValue("{\"name\":\"" + name + "\",\"workoutPartId\":" + partId + "}", WorkoutDto.Request.class);
    }

    private WorkoutPartDto.Request partRequest(String name) throws Exception {
        return objectMapper.readValue("{\"name\":\"" + name + "\"}", WorkoutPartDto.Request.class);
    }

    private String partJson(Long partId, Long workoutId) {
        return "{\"workoutPartId\":" + partId + ",\"exercises\":[{\"workoutId\":" + workoutId
                + ",\"sets\":[{\"setNumber\":1,\"reps\":10,\"restTime\":60}]}]}";
    }

    private WorkoutProgramDto.Request programRequest(Long partId, Long workoutId) throws Exception {
        return objectMapper.readValue("{\"name\":\"프로그램\",\"description\":\"\",\"parts\":[" + partJson(partId, workoutId) + "]}",
                WorkoutProgramDto.Request.class);
    }

    private WorkoutSessionDto.StartRequest startRequest(Long programId, Long workoutId) throws Exception {
        return objectMapper.readValue("{\"workoutProgramId\":" + programId + ",\"customExercises\":[{\"workoutId\":" + workoutId
                + ",\"order\":1,\"sets\":[{\"reps\":10,\"restTime\":60}]}]}", WorkoutSessionDto.StartRequest.class);
    }
}
