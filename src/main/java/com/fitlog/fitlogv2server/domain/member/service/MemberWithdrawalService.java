package com.fitlog.fitlogv2server.domain.member.service;

import com.fitlog.fitlogv2server.domain.auth.service.AuthService;
import com.fitlog.fitlogv2server.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원 탈퇴: 회원과 관련된 개인정보·운동 데이터·로그인 세션을 한 트랜잭션에서 모두 파기한다.
 * 중간에 실패하면 전부 롤백되어 일부만 지워진 상태가 남지 않는다.
 *
 * 삭제 순서 (FK 의존 순서)
 *  1. 운동 기록: 세트 → 세션 운동 → 세션
 *  2. 프로그램: 세트 → 운동 → 부위 → 프로그램
 *  3. 개인 운동 종목 → 개인 부위
 *  4. 로그인 세션·교환 코드
 *  5. 회원
 *
 * 참고: DB 백업에는 백업 보존 기간 동안 데이터가 남을 수 있다 (개인정보처리방침에 고지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;

    @Transactional
    public void withdraw(Long memberId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE id = ?", Integer.class, memberId);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }

        // 1) 운동 기록
        jdbcTemplate.update("""
                DELETE FROM workout_session_set WHERE workout_session_exercise_id IN (
                    SELECT wse.id FROM workout_session_exercise wse
                    JOIN workout_session ws ON ws.id = wse.workout_session_id
                    WHERE ws.member_id = ?)
                """, memberId);
        jdbcTemplate.update("""
                DELETE FROM workout_session_exercise WHERE workout_session_id IN (
                    SELECT id FROM workout_session WHERE member_id = ?)
                """, memberId);
        jdbcTemplate.update("DELETE FROM workout_session WHERE member_id = ?", memberId);

        // 2) 프로그램 (소프트 삭제된 프로그램 포함)
        jdbcTemplate.update("""
                DELETE FROM workout_program_set WHERE workout_program_exercise_id IN (
                    SELECT pe.id FROM workout_program_exercise pe
                    JOIN workout_program_part pp ON pp.id = pe.workout_program_part_id
                    JOIN workout_program p ON p.id = pp.workout_program_id
                    WHERE p.member_id = ?)
                """, memberId);
        jdbcTemplate.update("""
                DELETE FROM workout_program_exercise WHERE workout_program_part_id IN (
                    SELECT pp.id FROM workout_program_part pp
                    JOIN workout_program p ON p.id = pp.workout_program_id
                    WHERE p.member_id = ?)
                """, memberId);
        jdbcTemplate.update("""
                DELETE FROM workout_program_part WHERE workout_program_id IN (
                    SELECT id FROM workout_program WHERE member_id = ?)
                """, memberId);
        jdbcTemplate.update("DELETE FROM workout_program WHERE member_id = ?", memberId);

        // 3) 개인 운동/부위. 본인 데이터는 위에서 지웠으므로 남은 참조는 다른 회원의 데이터다.
        //    (소유권 검사 도입 전 생성된 교차 참조가 있을 수 있음) 이 경우 자동 파기하지 않고 전체를 롤백한다.
        Boolean referencedByOthers = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM workout_session_exercise wse JOIN workout w ON w.id = wse.workout_id WHERE w.member_id = ?)
                    OR EXISTS (SELECT 1 FROM workout_program_exercise pe JOIN workout w ON w.id = pe.workout_id WHERE w.member_id = ?)
                    OR EXISTS (SELECT 1 FROM workout_program_part pp JOIN workout_part p ON p.id = pp.workout_part_id WHERE p.member_id = ?)
                    OR EXISTS (SELECT 1 FROM workout w JOIN workout_part p ON p.id = w.workout_part_id
                               WHERE p.member_id = ? AND (w.member_id IS NULL OR w.member_id <> ?))
                """, Boolean.class, memberId, memberId, memberId, memberId, memberId);
        if (Boolean.TRUE.equals(referencedByOthers)) {
            log.warn("Withdrawal blocked: personal workouts/parts of member {} are referenced by other members' data", memberId);
            throw new ConflictException("다른 회원의 데이터와 연결된 항목이 있어 탈퇴를 자동으로 완료할 수 없습니다. 고객 문의로 요청해주세요.");
        }
        jdbcTemplate.update("DELETE FROM workout WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM workout_part WHERE member_id = ?", memberId);

        // 4) 로그인 세션·교환 코드 (모든 기기 로그아웃)
        authService.revokeAllSessions(memberId);

        // 5) 회원
        jdbcTemplate.update("DELETE FROM member WHERE id = ?", memberId);
        log.info("Member {} withdrew; all personal data deleted", memberId);
    }
}
