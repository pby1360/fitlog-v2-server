-- V2: 공용(마스터) 운동 부위·종목 시드 (기존 data.sql 이관)
-- 이미 존재하는 공용 데이터는 건너뛴다. 기존 운영 DB에서도 누락분만 채워진다.

-- WorkoutPart 데이터 삽입 (존재하지 않으면 삽입)
INSERT INTO workout_part (name) SELECT '가슴' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '가슴' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '등' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '등' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '어깨' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '어깨' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '팔' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '팔' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '복근' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '복근' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '하체' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '하체' AND member_id IS NULL);
INSERT INTO workout_part (name) SELECT '유산소' WHERE NOT EXISTS (SELECT 1 FROM workout_part WHERE name = '유산소' AND member_id IS NULL);

-- Workout 데이터 삽입 (존재하지 않으면 삽입)
-- 각 workout_part_id는 해당 workout_part의 id를 참조해야 함.
-- 서브쿼리를 사용하여 동적으로 ID를 가져옵니다.

-- 가슴 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '벤치프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '벤치프레스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '인클라인 벤치프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '인클라인 벤치프레스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '딥스', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '딥스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '푸시업', wp.id, NULL FROM workout_part wp WHERE wp.name = '가슴' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '푸시업' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 등 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '데드리프트', wp.id, NULL FROM workout_part wp WHERE wp.name = '등' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '데드리프트' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '풀업', wp.id, NULL FROM workout_part wp WHERE wp.name = '등' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '풀업' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '바벨로우', wp.id, NULL FROM workout_part wp WHERE wp.name = '등' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '바벨로우' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '랫풀다운', wp.id, NULL FROM workout_part wp WHERE wp.name = '등' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '랫풀다운' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 어깨 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '숄더프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '숄더프레스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '사이드레터럴레이즈', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '사이드레터럴레이즈' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '리어델트플라이', wp.id, NULL FROM workout_part wp WHERE wp.name = '어깨' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '리어델트플라이' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 팔 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '바이셉컬', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '바이셉컬' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '트라이셉딥스', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '트라이셉딥스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '해머컬', wp.id, NULL FROM workout_part wp WHERE wp.name = '팔' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '해머컬' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 복근 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '크런치', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '크런치' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '플랭크', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '플랭크' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '러시안트위스트', wp.id, NULL FROM workout_part wp WHERE wp.name = '복근' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '러시안트위스트' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 하체 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '스쿼트', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '스쿼트' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '런지', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '런지' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '레그프레스', wp.id, NULL FROM workout_part wp WHERE wp.name = '하체' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '레그프레스' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

-- 유산소 운동
INSERT INTO workout (name, workout_part_id, member_id)
SELECT '러닝머신', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '러닝머신' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '사이클', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '사이클' AND w.workout_part_id = wp.id AND w.member_id IS NULL);

INSERT INTO workout (name, workout_part_id, member_id)
SELECT '로잉머신', wp.id, NULL FROM workout_part wp WHERE wp.name = '유산소' AND wp.member_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM workout w WHERE w.name = '로잉머신' AND w.workout_part_id = wp.id AND w.member_id IS NULL);
