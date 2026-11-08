-- classmento 초기 스키마. 이후 변경은 V2__..., V3__... 새 파일로만 (적용된 파일 수정 금지).
CREATE EXTENSION IF NOT EXISTS vector;

-- 회원 ------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),                 -- 소셜 전용 계정은 NULL
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,        -- USER | ADMIN
    provider      VARCHAR(20)  NOT NULL,        -- LOCAL | GOOGLE | KAKAO
    provider_id   VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_provider UNIQUE (provider, provider_id)
);

-- 이력서 ----------------------------------------------------------------
CREATE TABLE resume (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES app_user (id),
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    status            VARCHAR(20)  NOT NULL,    -- UPLOADED | EXTRACTED | PARSED | FAILED
    failure_reason    VARCHAR(500),
    extracted_text    TEXT,
    parsed_json       JSONB,
    parse_model       VARCHAR(100),
    prompt_version    VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_resume_user ON resume (user_id, created_at DESC);

-- 이력서 청크 + 임베딩 (RAG). 차원 1536 = OpenAI text-embedding-3-small
CREATE TABLE resume_chunk (
    id         BIGSERIAL PRIMARY KEY,
    resume_id  BIGINT      NOT NULL REFERENCES resume (id) ON DELETE CASCADE,
    seq        INT         NOT NULL,
    content    TEXT        NOT NULL,
    embedding  vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resume_chunk_seq UNIQUE (resume_id, seq)
);
CREATE INDEX ix_resume_chunk_embedding ON resume_chunk USING hnsw (embedding vector_cosine_ops);

-- Spring Session JDBC (로그인 세션 저장) -----------------------------------
CREATE TABLE spring_session (
    primary_id            CHAR(36)     NOT NULL,
    session_id            CHAR(36)     NOT NULL,
    creation_time         BIGINT       NOT NULL,
    last_access_time      BIGINT       NOT NULL,
    max_inactive_interval INT          NOT NULL,
    expiry_time           BIGINT       NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
