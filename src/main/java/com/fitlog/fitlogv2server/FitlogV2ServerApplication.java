package com.fitlog.fitlogv2server;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
public class FitlogV2ServerApplication {

    /**
     * 서버가 어느 지역에서 실행되든 모든 시각을 UTC 기준으로 저장하도록
     * JVM 기본 시간대를 UTC로 고정한다. (JPA Auditing의 createdAt/updatedAt 포함)
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(FitlogV2ServerApplication.class, args);
    }

}
