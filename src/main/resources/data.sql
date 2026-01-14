-- WorkoutPart 데이터 삽입 (존재하지 않으면 삽입)
INSERT INTO workout_part (name) VALUES ('가슴') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('등') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('어깨') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('팔') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('복근') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('하체') ON CONFLICT (name) DO NOTHING;
INSERT INTO workout_part (name) VALUES ('유산소') ON CONFLICT (name) DO NOTHING;

-- Workout 데이터 삽입 (존재하지 않으면 삽입)
-- 각 workout_part_id는 해당 workout_part의 id를 참조해야 함.
-- 서브쿼리를 사용하여 동적으로 ID를 가져옵니다.

-- 가슴 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '벤치프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '인클라인 벤치프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '딥스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '푸시업', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴'
ON CONFLICT DO NOTHING;

-- 등 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '데드리프트', wp.id, NULL FROM workout_part wp WHERE wp.name = '등'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '풀업', wp.id, NULL FROM workout_part wp WHERE wp.name = '등'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '바벨로우', wp.id, NULL FROM workout_part wp WHERE wp.name = '등'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '랫풀다운', wp.id, NULL FROM workout_part wp WHERE wp.name = '등'
ON CONFLICT DO NOTHING;

-- 어깨 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '숄더프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '사이드레터럴레이즈', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '리어델트플라이', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨'
ON CONFLICT DO NOTHING;

-- 팔 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '바이셉컬', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '트라이셉딥스', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '해머컬', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔'
ON CONFLICT DO NOTHING;

-- 복근 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '크런치', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '플랭크', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '러시안트위스트', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근'
ON CONFLICT DO NOTHING;

-- 하체 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '스쿼트', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '런지', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '레그프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체'
ON CONFLICT DO NOTHING;

-- 유산소 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '러닝머신', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '사이클', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소'
ON CONFLICT DO NOTHING;

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '로잉머신', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소'
ON CONFLICT DO NOTHING;
