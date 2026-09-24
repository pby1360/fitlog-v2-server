package com.fitlog.fitlogv2server.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 실제 PostgreSQL(임베디드 바이너리) 위에서 Flyway 마이그레이션을 적용한 뒤 스프링 컨텍스트를 띄운다.
 * Docker가 필요 없으므로 로컬과 CI에서 동일하게 동작한다.
 * JVM 당 한 번만 기동하고, 테스트 클래스 간에 공유한다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setServerConfig("timezone", "UTC")
                    .start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("임베디드 PostgreSQL 기동 실패", e);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }
}
