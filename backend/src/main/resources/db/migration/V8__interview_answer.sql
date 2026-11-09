-- 면접 답변(녹음 → STT → 평가)과 기준별 점수. 답변은 질문마다 하나(다시 녹음하면 같은 행을 고친다).
-- audio_version: 다시 녹음·재시도마다 오른다. 백그라운드 처리는 시작할 때의 버전과 다르면 결과를 버린다.

CREATE TABLE interview_answer (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES interview_session (id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    seq             INT          NOT NULL,
    kind            VARCHAR(20)  NOT NULL,       -- TECH | BEHAVIORAL (루브릭)
    audio_key       VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    audio_version   INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL,       -- UPLOADED | TRANSCRIBED | EVALUATED | FAILED
    transcript      TEXT,
    duration_millis BIGINT       NOT NULL DEFAULT 0,
    failure_reason  VARCHAR(300),
    overall_score   DOUBLE PRECISION,
    strength        VARCHAR(500),
    improvement     VARCHAR(500),
    suggestion      VARCHAR(500),
    prompt_version  VARCHAR(100),
    model           VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_interview_answer_seq UNIQUE (session_id, seq)
);
CREATE INDEX ix_interview_answer_user ON interview_answer (user_id);

CREATE TABLE interview_answer_score (
    answer_id BIGINT      NOT NULL REFERENCES interview_answer (id) ON DELETE CASCADE,
    criterion VARCHAR(30) NOT NULL,
    score     INT         NOT NULL CHECK (score BETWEEN 1 AND 5),
    evidence  TEXT,
    PRIMARY KEY (answer_id, criterion)
);
