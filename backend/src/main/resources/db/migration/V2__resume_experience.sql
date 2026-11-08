-- 이력서 경험(경력·프로젝트 하나) + 임베딩. 경험 기반 면접(ADR 0005)에서 사용자가 고르는 단위.
CREATE TABLE resume_experience (
    id           BIGSERIAL PRIMARY KEY,
    resume_id    BIGINT       NOT NULL REFERENCES resume (id) ON DELETE CASCADE,
    seq          INT          NOT NULL,
    type         VARCHAR(20)  NOT NULL,          -- WORK | PROJECT
    title        VARCHAR(100) NOT NULL,
    organization VARCHAR(200),
    period       VARCHAR(100),
    summary      VARCHAR(200) NOT NULL,
    detail       TEXT,
    skills       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    embedding    vector(1536) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_resume_experience_seq UNIQUE (resume_id, seq)
);
CREATE INDEX ix_resume_experience_embedding ON resume_experience USING hnsw (embedding vector_cosine_ops);
