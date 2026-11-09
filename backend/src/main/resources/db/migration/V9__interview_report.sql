-- 면접 종합 리포트(모든 답변 평가가 끝난 뒤 session-report 프롬프트로 한 번 만든다).
ALTER TABLE interview_session
    ADD COLUMN report_overall        DOUBLE PRECISION,
    ADD COLUMN report_strength       VARCHAR(500),
    ADD COLUMN report_improvement    VARCHAR(500),
    ADD COLUMN report_prompt_version VARCHAR(100),
    ADD COLUMN report_model          VARCHAR(100);
