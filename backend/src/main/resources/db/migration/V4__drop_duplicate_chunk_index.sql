-- V3에서 만든 ix_resume_chunk_resume (resume_id, seq)는 V1의 uq_resume_chunk_seq UNIQUE (resume_id, seq)가
-- 이미 만든 인덱스와 같다. 중복 인덱스는 쓰기 비용만 늘리므로 지운다.
DROP INDEX IF EXISTS ix_resume_chunk_resume;
