-- 꼬리질문(ADR 0008): 세션의 옵션(질문 하나에 최대 0~2개)과 질문의 사슬 정보.
-- 질문 번호(seq)는 고정 식별자로 두고 꼬리는 뒤 번호로 붙인다. 화면 순서는 (root_seq, depth).
ALTER TABLE interview_session ADD COLUMN max_follow_ups INT NOT NULL DEFAULT 0;

ALTER TABLE interview_question ADD COLUMN parent_seq INT;
ALTER TABLE interview_question ADD COLUMN root_seq INT;
ALTER TABLE interview_question ADD COLUMN depth INT NOT NULL DEFAULT 0;
ALTER TABLE interview_question ADD COLUMN follow_up VARCHAR(20) NOT NULL DEFAULT 'NONE';

-- 이미 있는 질문은 모두 본 질문이다
UPDATE interview_question SET root_seq = seq;
ALTER TABLE interview_question ALTER COLUMN root_seq SET NOT NULL;

ALTER TABLE interview_session
  ADD CONSTRAINT ck_interview_session_max_follow_ups CHECK (max_follow_ups BETWEEN 0 AND 2);
ALTER TABLE interview_question
  ADD CONSTRAINT ck_interview_question_depth CHECK (depth BETWEEN 0 AND 2);
