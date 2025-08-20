-- 스키마 업데이트 마이그레이션 스크립트
-- Railway 데이터베이스에서 실행

-- 1. 외래키 제약조건 제거 (존재하는 경우)
SET FOREIGN_KEY_CHECKS = 0;

-- 2. sequence_data 테이블의 file_id 컬럼 타입 변경
ALTER TABLE sequence_data MODIFY COLUMN file_id VARCHAR(255) NOT NULL;

-- 3. uploaded_files 테이블의 file_id에 UNIQUE 제약조건 추가
ALTER TABLE uploaded_files MODIFY COLUMN file_id VARCHAR(255) UNIQUE NOT NULL;

-- 4. 새로운 외래키 제약조건 추가
ALTER TABLE sequence_data 
ADD CONSTRAINT fk_sequence_file_id 
FOREIGN KEY (file_id) REFERENCES uploaded_files(file_id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;