-- V7: 개인정보 최소 수집 (#13)
-- 서비스 기능에 쓰이지 않는 전화번호·생년월일은 더 이상 수집하지 않으며, 기존 저장값도 파기한다.
-- 주의: 되돌릴 수 없다. 필요하면 배포 전에 백업/보존 의무 여부를 확인할 것.
ALTER TABLE member DROP COLUMN IF EXISTS phone;
ALTER TABLE member DROP COLUMN IF EXISTS birth_date;
