package com.fitlog.fitlogv2server.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * OAuth 로그인 직후 프론트로 전달하는 일회용 교환 코드 저장소 (코드는 해시로만 저장).
 */
@Repository
@RequiredArgsConstructor
public class LoginCodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(String codeHash, long memberId, Instant expiresAt) {
        jdbcTemplate.update("DELETE FROM auth_login_code WHERE expires_at <= now()");
        jdbcTemplate.update("INSERT INTO auth_login_code (code_hash, member_id, expires_at) VALUES (?, ?, ?)",
                codeHash, memberId, Timestamp.from(expiresAt));
    }

    /**
     * 코드를 소비한다. 삭제와 조회를 한 문장으로 처리하므로 같은 코드는 한 번만 사용할 수 있다.
     */
    public Optional<Long> consume(String codeHash) {
        return jdbcTemplate.queryForList(
                        "DELETE FROM auth_login_code WHERE code_hash = ? AND expires_at > now() RETURNING member_id",
                        Long.class, codeHash)
                .stream().findFirst();
    }

    public void deleteAllByMember(long memberId) {
        jdbcTemplate.update("DELETE FROM auth_login_code WHERE member_id = ?", memberId);
    }
}
