-- V3: 운동 세션 무결성 (#6, #9, #23 일부)
-- 기존 운영 데이터가 새 제약을 위반하지 않도록 먼저 정리한 뒤 제약을 추가한다.

-- 1) 사용자당 활성(진행/일시정지) 세션은 1개
--    중복된 활성 세션이 있으면 가장 최근(id 최대) 1건만 남기고 나머지는 취소 처리한다.
UPDATE workout_session ws
SET status = 'CANCELLED',
    end_time = COALESCE(ws.end_time, now()),
    last_paused_at = NULL
WHERE ws.status IN ('IN_PROGRESS', 'PAUSED')
  AND ws.id < (SELECT MAX(o.id)
               FROM workout_session o
               WHERE o.member_id = ws.member_id
                 AND o.status IN ('IN_PROGRESS', 'PAUSED'));

CREATE UNIQUE INDEX ux_workout_session_active_member
    ON workout_session (member_id)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');

-- 2) 세트 완료 여부는 NULL 불가 (조회 시 언박싱 오류 방지)
UPDATE workout_session_set SET completed = false WHERE completed IS NULL;
ALTER TABLE workout_session_set ALTER COLUMN completed SET DEFAULT false;
ALTER TABLE workout_session_set ALTER COLUMN completed SET NOT NULL;

-- 3) 운동별 세트 번호는 NULL 불가 + 중복 불가
--    NULL 또는 중복 번호가 있는 운동만 (기존 번호, id) 순서로 1부터 다시 매긴다.
WITH targets AS (
    SELECT workout_session_exercise_id
    FROM workout_session_set
    GROUP BY workout_session_exercise_id
    HAVING COUNT(*) FILTER (WHERE set_number IS NULL) > 0
        OR COUNT(set_number) <> COUNT(DISTINCT set_number)
), renumbered AS (
    SELECT s.id,
           ROW_NUMBER() OVER (PARTITION BY s.workout_session_exercise_id
                              ORDER BY s.set_number NULLS LAST, s.id) AS rn
    FROM workout_session_set s
    JOIN targets t ON t.workout_session_exercise_id = s.workout_session_exercise_id
)
UPDATE workout_session_set s
SET set_number = r.rn
FROM renumbered r
WHERE s.id = r.id;

ALTER TABLE workout_session_set ALTER COLUMN set_number SET NOT NULL;
ALTER TABLE workout_session_set
    ADD CONSTRAINT uk_workout_session_set_exercise_number UNIQUE (workout_session_exercise_id, set_number);

-- 4) 음수 값 금지. 기존 행은 검사하지 않고(NOT VALID) 새로 쓰는 값부터 적용한다.
ALTER TABLE workout_session_set
    ADD CONSTRAINT ck_workout_session_set_non_negative CHECK (
        (weight IS NULL OR weight >= 0)
        AND (reps IS NULL OR reps >= 0)
        AND (rest_time IS NULL OR rest_time >= 0)
        AND (actual_weight IS NULL OR actual_weight >= 0)
        AND (actual_reps IS NULL OR actual_reps >= 0)
    ) NOT VALID;

ALTER TABLE workout_program_set
    ADD CONSTRAINT ck_workout_program_set_non_negative CHECK (
        (weight IS NULL OR weight >= 0)
        AND (reps IS NULL OR reps >= 0)
        AND (rest_time IS NULL OR rest_time >= 0)
    ) NOT VALID;

-- 5) 조회 인덱스: 기록/통계 조회(회원+상태+시작시각)와 세션 → 운동 조인
CREATE INDEX IF NOT EXISTS ix_workout_session_member_status_start
    ON workout_session (member_id, status, start_time);
CREATE INDEX IF NOT EXISTS ix_workout_session_exercise_session
    ON workout_session_exercise (workout_session_id);
