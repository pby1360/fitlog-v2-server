-- V5: 운동 종목/부위 마스터의 소유자별 유일성, 보관(archive), 기록 이름 스냅샷 (#12, #21, #22)

-- 1) 보관 컬럼: 과거 기록·프로그램이 참조하는 개인 종목/부위는 물리 삭제 대신 보관한다
ALTER TABLE workout ADD COLUMN archived_at timestamptz;
ALTER TABLE workout_part ADD COLUMN archived_at timestamptz;

-- 2) 사용자 구분 없는 기존 UNIQUE 제거
--    workout_part.name 의 UNIQUE 는 생성 방식에 따라 이름이 다를 수 있어 컬럼 기준으로 찾아 제거한다.
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE rel.relname = 'workout_part'
          AND con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND att.attname = 'name'
    LOOP
        EXECUTE format('ALTER TABLE workout_part DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE workout DROP CONSTRAINT IF EXISTS workout_name_part_id_unique;

-- 3) 소유자 범위의 부분 유니크 인덱스 (보관된 항목은 제외 → 같은 이름으로 다시 만들 수 있음)
--    공용: 공용끼리만 유일 / 개인: 같은 회원 안에서만 유일
CREATE UNIQUE INDEX ux_workout_part_public_name
    ON workout_part (name) WHERE member_id IS NULL AND archived_at IS NULL;
CREATE UNIQUE INDEX ux_workout_part_member_name
    ON workout_part (member_id, name) WHERE member_id IS NOT NULL AND archived_at IS NULL;
CREATE UNIQUE INDEX ux_workout_public_name
    ON workout (workout_part_id, name) WHERE member_id IS NULL AND archived_at IS NULL;
CREATE UNIQUE INDEX ux_workout_member_name
    ON workout (member_id, workout_part_id, name) WHERE member_id IS NOT NULL AND archived_at IS NULL;

-- 4) 과거 기록 표기 고정: 세션 생성 시점의 프로그램/운동/부위 이름을 저장한다
ALTER TABLE workout_session ADD COLUMN program_name varchar(255);
ALTER TABLE workout_session_exercise ADD COLUMN workout_name varchar(255);
ALTER TABLE workout_session_exercise ADD COLUMN body_part_name varchar(255);

-- 기존 기록은 현재 이름으로 채운다 (이 시점 이후의 이름 변경부터 기록에 영향 없음)
UPDATE workout_session ws
SET program_name = p.name
FROM workout_program p
WHERE ws.workout_program_id = p.id AND ws.program_name IS NULL;

UPDATE workout_session_exercise wse
SET workout_name = w.name,
    body_part_name = wp.name
FROM workout w
JOIN workout_part wp ON wp.id = w.workout_part_id
WHERE wse.workout_id = w.id AND wse.workout_name IS NULL;

-- 5) 삭제 전 참조 여부 확인용 인덱스
CREATE INDEX IF NOT EXISTS ix_workout_program_exercise_workout ON workout_program_exercise (workout_id);
CREATE INDEX IF NOT EXISTS ix_workout_session_exercise_workout ON workout_session_exercise (workout_id);
CREATE INDEX IF NOT EXISTS ix_workout_program_part_workout_part ON workout_program_part (workout_part_id);
CREATE INDEX IF NOT EXISTS ix_workout_workout_part ON workout (workout_part_id);
