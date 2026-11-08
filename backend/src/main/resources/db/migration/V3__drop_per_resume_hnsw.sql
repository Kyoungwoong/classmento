-- 이력서별 kNN(WHERE resume_id = ?)에는 전역 HNSW 인덱스를 쓰지 않는다.
-- 데이터가 많아져 플래너가 HNSW를 고르면 ef_search(기본 40)개 후보를 먼저 뽑고 나서 resume_id로 거르기 때문에
-- 결과가 k개보다 적거나 0개가 될 수 있다. 이력서 하나의 청크·경험은 수십 개라 resume_id로 좁힌 뒤
-- 정확히 정렬하는 편이 빠르고 정확하다. (전역 kNN이 필요한 질문 은행 등은 HNSW를 둔다)
DROP INDEX IF EXISTS ix_resume_experience_embedding;
DROP INDEX IF EXISTS ix_resume_chunk_embedding;

CREATE INDEX IF NOT EXISTS ix_resume_chunk_resume ON resume_chunk (resume_id, seq);
