package com.fitlog.fitlogv2server.domain.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.domain.auth.service.AuthService;
import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.service.MemberWithdrawalService;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutDto;
import com.fitlog.fitlogv2server.domain.workout.dto.WorkoutPartDto;
import com.fitlog.fitlogv2server.domain.workout.service.WorkoutService;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import com.fitlog.fitlogv2server.domain.workoutprogram.service.WorkoutProgramService;
import com.fitlog.fitlogv2server.domain.workoutsession.dto.WorkoutSessionDto;
import com.fitlog.fitlogv2server.domain.workoutsession.service.WorkoutSessionService;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberWithdrawalIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberWithdrawalService memberWithdrawalService;
    @Autowired
    private WorkoutService workoutService;
    @Autowired
    private WorkoutProgramService workoutProgramService;
    @Autowired
    private WorkoutSessionService workoutSessionService;
    @Autowired
    private AuthService authService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void withdrawal_deletesAllPersonalData_andKeepsOthersUntouched() throws Exception {
        Member member = fixtures.member();
        Member other = fixtures.member();
        Long workoutId = createFullDataSet(member);
        createFullDataSet(other);
        AuthService.TokenPair tokens = authService.issueTokens(member);
        authService.createLoginCode(member);

        memberWithdrawalService.withdraw(member.getId());

        long id = member.getId();
        assertThat(count("SELECT COUNT(*) FROM member WHERE id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM workout_session WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM workout_program WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM workout WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM workout_part WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM auth_session WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM auth_login_code WHERE member_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM workout_session_exercise WHERE workout_id = ?", workoutId)).isZero();
        assertThatThrownBy(() -> authService.reissue(tokens.refreshToken())).isInstanceOf(ResponseStatusException.class);

        // 다른 회원의 데이터는 그대로
        assertThat(count("SELECT COUNT(*) FROM workout_session WHERE member_id = ?", other.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM workout_program WHERE member_id = ?", other.getId())).isEqualTo(1);
    }

    @Test
    void withdrawal_isRolledBackWhenOtherMembersReferencePersonalItems() throws Exception {
        Member member = fixtures.member();
        Member other = fixtures.member();
        createFullDataSet(member);
        Long memberWorkoutId = jdbcTemplate.queryForObject("SELECT id FROM workout WHERE member_id = ?", Long.class, member.getId());
        Long otherProgramId = fixtures.emptyProgram(other).getId();
        // 소유권 검사 도입 전에 생길 수 있었던 교차 참조를 직접 만든다
        jdbcTemplate.update("INSERT INTO workout_session (member_id, workout_program_id, status, start_time, total_paused_seconds) " +
                "VALUES (?, ?, 'COMPLETED', now(), 0)", other.getId(), otherProgramId);
        Long otherSessionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM workout_session WHERE member_id = ?", Long.class, other.getId());
        jdbcTemplate.update("INSERT INTO workout_session_exercise (workout_session_id, workout_id, \"order\", skipped) VALUES (?, ?, 1, false)",
                otherSessionId, memberWorkoutId);

        assertThatThrownBy(() -> memberWithdrawalService.withdraw(member.getId())).isInstanceOf(ConflictException.class);

        // 일부만 지워지지 않고 전부 롤백된다
        assertThat(count("SELECT COUNT(*) FROM member WHERE id = ?", member.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM workout_session WHERE member_id = ?", member.getId())).isEqualTo(1);
    }

    // 회원 한 명의 전체 데이터: 개인 부위/운동, 그 운동을 쓰는 프로그램, 운동 기록 1건
    private Long createFullDataSet(Member member) throws Exception {
        workoutService.addWorkoutPart(objectMapper.readValue("{\"name\":\"개인부위\"}", WorkoutPartDto.Request.class), member);
        Long partId = jdbcTemplate.queryForObject("SELECT id FROM workout_part WHERE member_id = ?", Long.class, member.getId());
        workoutService.addWorkout(objectMapper.readValue("{\"name\":\"개인운동\",\"workoutPartId\":" + partId + "}", WorkoutDto.Request.class), member);
        Long workoutId = jdbcTemplate.queryForObject("SELECT id FROM workout WHERE member_id = ?", Long.class, member.getId());

        workoutProgramService.createWorkoutProgram(objectMapper.readValue(
                "{\"name\":\"프로그램\",\"description\":\"\",\"parts\":[{\"workoutPartId\":" + partId
                        + ",\"exercises\":[{\"workoutId\":" + workoutId + ",\"sets\":[{\"setNumber\":1,\"reps\":10,\"restTime\":60}]}]}]}",
                WorkoutProgramDto.Request.class), member);
        Long programId = jdbcTemplate.queryForObject("SELECT id FROM workout_program WHERE member_id = ?", Long.class, member.getId());

        workoutSessionService.startSession(member, objectMapper.readValue(
                "{\"workoutProgramId\":" + programId + "}", WorkoutSessionDto.StartRequest.class));
        return workoutId;
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result != null ? result : 0;
    }
}
