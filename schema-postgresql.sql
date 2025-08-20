-- PostgreSQL Schema for BioDataHub
-- 기존 테이블이 있다면 삭제
DROP TABLE IF EXISTS sequence_matches CASCADE;
DROP TABLE IF EXISTS sequence_data CASCADE;
DROP TABLE IF EXISTS uploaded_files CASCADE;

-- 업로드된 파일 정보를 저장하는 테이블
CREATE TABLE uploaded_files (
    id BIGSERIAL PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_type VARCHAR(20) DEFAULT 'COMPARISON',
    processing_status VARCHAR(20) DEFAULT 'PENDING'
);

-- 서열 데이터를 저장하는 테이블
CREATE TABLE sequence_data (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    sequence_id VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_content TEXT NOT NULL,
    sequence_length INTEGER NOT NULL,
    gc_content DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sequence_file FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

-- 서열 비교 결과를 저장하는 테이블
CREATE TABLE sequence_matches (
    id BIGSERIAL PRIMARY KEY,
    reference_sequence_id BIGINT NOT NULL,
    comparison_sequence_id BIGINT NOT NULL,
    similarity_score DECIMAL(5,2) NOT NULL,
    alignment_length INTEGER,
    match_count INTEGER,
    mismatch_count INTEGER,
    gap_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ref_sequence FOREIGN KEY (reference_sequence_id) REFERENCES sequence_data(id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_sequence FOREIGN KEY (comparison_sequence_id) REFERENCES sequence_data(id) ON DELETE CASCADE
);

-- 샘플 데이터
INSERT INTO uploaded_files (original_filename, stored_filename, file_path, file_size, content_type, file_type, processing_status) 
VALUES 
('sample_reference.fasta', 'ref_001.fasta', '/uploads/ref_001.fasta', 2048, 'text/plain', 'REFERENCE', 'COMPLETED'),
('sample_comparison1.fasta', 'comp_001.fasta', '/uploads/comp_001.fasta', 1856, 'text/plain', 'COMPARISON', 'COMPLETED'),
('sample_comparison2.fasta', 'comp_002.fasta', '/uploads/comp_002.fasta', 1920, 'text/plain', 'COMPARISON', 'COMPLETED');

INSERT INTO sequence_data (file_id, sequence_id, description, sequence_content, sequence_length, gc_content)
VALUES 
(1, 'REF_SEQ_001', 'Reference sequence for COVID-19 spike protein', 'ATGGTTCACCTGCAGAAGTTTGATGATGGCGATGGCGGCACCCACTATCGCCGCACAGGCTACGTGACTCTGCGCAAGGTTGAGCGCCAGGGCTTCCTGTTCGACATCACGGAGGACCGCACGCCCAACGTGAAACTGCCCAAGGGGTTGACACCAGGGTGCCGATCTGTTTCAGCGGGTGCCAGAACGGTAAGTACACAGACCCGGAACGCACTGGATATGGGGCACGTGACACCAGACCCCCGGTGGTTCGGCGACACCAAGGACACGTATCGGGACACTGACACAACCACCACC', 400, 52.25),
(2, 'COMP_SEQ_001', 'Comparison sequence variant Alpha', 'ATGGTTCACCTGCAGAAGTTTGATGATGGCGATGGCGGCACCCACTATCGCCGCACAGGCTACGTGACTCTGCGCAAGGTTGAGCGCCAGGGCTTCCTGTTCGACATCACGGAGGACCGCACGCCCAACGTGAAACTGCCCAAGGGGTTGACACCAGGGTGCCGATCTGTTTCAGCGGGTGCCAGAACGGTAAGTACACAGACCCGGAACGCACTGGATATGGGGCACGTGACACCAGACCCCCGGTGGTTCGGCGACACCAAGGACACGTATTGGGACACTGACACAACCACCACC', 400, 52.00),
(3, 'COMP_SEQ_002', 'Comparison sequence variant Beta', 'ATGGTTCACCTGCAGAAGTTTGATGATGGCGATGGCGGCACCCACTATCGCCGCACAGGCTACGTGACTCTGCGCAAGGTTGAGCGCCAGGGCTTCCTGTTCGACATCACGGAGGACCGCACGCCCAACGTGAAACTGCCCAAGGGGTTGACACCAGGGTGCCGATCTGTTTCAGCGGGTGCCAGAACGGTAAGTACACAGACCCGGAACGCACTGGATATGGGGCACGTGACACCAGACCCCCGGTGGTTCGGCGACACCAAGGACACGTATAGGGACACTGACACAACCACCACC', 400, 52.50);

INSERT INTO sequence_matches (reference_sequence_id, comparison_sequence_id, similarity_score, alignment_length, match_count, mismatch_count, gap_count)
VALUES 
(1, 2, 99.75, 400, 399, 1, 0),
(1, 3, 99.50, 400, 398, 2, 0);