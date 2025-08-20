-- 전체 스키마 재생성 (기존 데이터 삭제됨)
-- Railway에서 실행

-- 기존 테이블 삭제
DROP TABLE IF EXISTS sequence_matches;
DROP TABLE IF EXISTS sequence_data;
DROP TABLE IF EXISTS uploaded_files;

-- 새로운 테이블 생성
CREATE TABLE uploaded_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id VARCHAR(255) UNIQUE NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20) DEFAULT 'COMPARISON',
    upload_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sequence_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL,
    sequence_id VARCHAR(255) NOT NULL,
    header TEXT,
    sequence TEXT NOT NULL,
    sequence_length INT NOT NULL,
    gc_content DECIMAL(5,2),
    a_count INT DEFAULT 0,
    t_count INT DEFAULT 0,
    c_count INT DEFAULT 0,
    g_count INT DEFAULT 0,
    n_count INT DEFAULT 0,
    is_valid BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES uploaded_files(file_id) ON DELETE CASCADE
);

CREATE TABLE sequence_matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_sequence_id BIGINT NOT NULL,
    comparison_sequence_id BIGINT NOT NULL,
    similarity_score DECIMAL(5,2) NOT NULL,
    alignment_length INT,
    match_count INT,
    mismatch_count INT,
    gap_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reference_sequence_id) REFERENCES sequence_data(id) ON DELETE CASCADE,
    FOREIGN KEY (comparison_sequence_id) REFERENCES sequence_data(id) ON DELETE CASCADE
);