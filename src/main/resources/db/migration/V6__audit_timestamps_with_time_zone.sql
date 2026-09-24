-- V6: 감사 시각(created_at/updated_at)과 프로그램 삭제 시각을 시간대 포함 타입(timestamptz)으로 변환 (#35)
--
-- 기존 값은 시간대 없이 "JVM 기본 시간대의 벽시계 시각"으로 저장돼 있었다.
-- 운영 컨테이너(eclipse-temurin alpine)는 별도 TZ 설정이 없으면 UTC 이므로 기본값을 UTC 로 해석한다.
-- 운영에 TZ 가 따로 설정돼 있었다면 배포 전에 LEGACY_TIMESTAMP_ZONE 환경변수로 그 시간대를 지정해야 한다.
--   (application.yml 의 spring.flyway.placeholders.legacy_timestamp_zone)

ALTER TABLE member
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE '${legacy_timestamp_zone}',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE '${legacy_timestamp_zone}';

ALTER TABLE workout_program
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE '${legacy_timestamp_zone}',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE '${legacy_timestamp_zone}',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE '${legacy_timestamp_zone}';

ALTER TABLE workout_session
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE '${legacy_timestamp_zone}',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE '${legacy_timestamp_zone}';
