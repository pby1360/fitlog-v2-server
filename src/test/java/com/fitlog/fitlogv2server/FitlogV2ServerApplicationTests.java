package com.fitlog.fitlogv2server;

import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 빈 DB에 Flyway 마이그레이션을 적용한 뒤 Hibernate validate가 통과하는지 확인한다.
 * 엔티티를 바꾸고 마이그레이션을 추가하지 않으면 이 테스트가 실패한다.
 */
class FitlogV2ServerApplicationTests extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void masterDataIsSeeded() {
        Integer parts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout_part WHERE member_id IS NULL", Integer.class);
        Integer workouts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout WHERE member_id IS NULL", Integer.class);

        assertThat(parts).isEqualTo(7);
        assertThat(workouts).isEqualTo(23);
    }
}
