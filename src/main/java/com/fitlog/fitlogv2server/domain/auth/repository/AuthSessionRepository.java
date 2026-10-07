package com.fitlog.fitlogv2server.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 로그인 세션(auth_session) 저장소. 토큰 회전을 하나의 조건부 UPDATE로 처리하기 위해 JDBC를 사용한다.
 * Refresh 토큰은 모두 SHA-256 해시로만 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class AuthSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public record RotatedFrom(long sessionId, Instant rotatedAt) {
    }

    public void create(long memberId, String refreshTokenHash, Instant expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO auth_session (member_id, refresh_token_hash, expires_at) VALUES (?, ?, ?)",
                memberId, refreshTokenHash, Timestamp.from(expiresAt));
    }

    /**
     * 유효한 세션의 토큰을 원자적으로 교체한다. 같은 토큰으로 동시에 요청하면 하나만 성공한다.
     *
     * @return 교체에 성공한 세션의 회원 ID
     */
    public Optional<Long> rotate(String currentHash, String newHash, long expectedMemberId, Instant newExpiresAt) {
        List<Long> memberIds = jdbcTemplate.queryForList("""
                        UPDATE auth_session
                        SET refresh_token_hash = ?, previous_token_hash = refresh_token_hash,
                            rotated_at = now(), last_used_at = now(), expires_at = ?
                        WHERE refresh_token_hash = ? AND member_id = ?
                          AND revoked_at IS NULL AND expires_at > now()
                        RETURNING member_id
                        """,
                Long.class, newHash, Timestamp.from(newExpiresAt), currentHash, expectedMemberId);
        return memberIds.stream().findFirst();
    }

    // 이미 회전된(이전) 토큰으로 요청이 들어왔는지 확인 (재사용 감지)
    public Optional<RotatedFrom> findActiveByPreviousHash(String previousHash) {
        return jdbcTemplate.query("""
                        SELECT id, rotated_at FROM auth_session
                        WHERE previous_token_hash = ? AND revoked_at IS NULL
                        """,
                (rs, rowNum) -> new RotatedFrom(rs.getLong("id"), rs.getTimestamp("rotated_at").toInstant()),
                previousHash).stream().findFirst();
    }

    public void revokeById(long sessionId) {
        jdbcTemplate.update("UPDATE auth_session SET revoked_at = now() WHERE id = ? AND revoked_at IS NULL", sessionId);
    }

    public int revokeByHash(String refreshTokenHash) {
        return jdbcTemplate.update(
                "UPDATE auth_session SET revoked_at = now() WHERE refresh_token_hash = ? AND revoked_at IS NULL",
                refreshTokenHash);
    }

    // 회원의 만료·폐기된 세션 행 정리 (로그인 시 호출)
    public void deleteInactiveByMember(long memberId) {
        jdbcTemplate.update(
                "DELETE FROM auth_session WHERE member_id = ? AND (revoked_at IS NOT NULL OR expires_at <= now())",
                memberId);
    }

    public void deleteAllByMember(long memberId) {
        jdbcTemplate.update("DELETE FROM auth_session WHERE member_id = ?", memberId);
    }
}
