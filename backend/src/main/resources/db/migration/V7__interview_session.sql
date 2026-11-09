-- 면접 세션과 질문. 질문은 세션을 만들 때 모두 정해 둔다(꼬리질문 없음).
-- resume_id·experience_id·bank_question_id는 다른 컨텍스트의 ID라 FK로 묶지 않는다(이력서를 지워도 면접 기록은 남는다).
-- 답변·평가 테이블은 답변 기능(E3)에서 V8로 추가한다.

CREATE TABLE interview_session (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type          VARCHAR(20) NOT NULL,           -- EXPERIENCE | TECH | BEHAVIORAL | MIXED
    level         VARCHAR(10),                    -- NEW | JUNIOR | MIDDLE | SENIOR
    job_role      VARCHAR(20),
    resume_id     BIGINT,
    experience_id BIGINT,
    status        VARCHAR(20) NOT NULL,           -- IN_PROGRESS | FINISHED | REPORTED
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_interview_session_user ON interview_session (user_id, created_at DESC);

CREATE TABLE interview_question (
    session_id       BIGINT       NOT NULL REFERENCES interview_session (id) ON DELETE CASCADE,
    seq              INT          NOT NULL,
    text             VARCHAR(300) NOT NULL,
    intent           VARCHAR(200),
    kind             VARCHAR(20)  NOT NULL,       -- TECH | BEHAVIORAL (평가 루브릭)
    bank_question_id BIGINT,
    generated        BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (session_id, seq)
);
