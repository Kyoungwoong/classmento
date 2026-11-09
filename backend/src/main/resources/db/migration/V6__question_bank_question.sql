-- 질문 은행: 질문 + 직무 + 태그 + 임베딩.
-- embedding은 전역 kNN(경험 기반 면접에서 경험 벡터로 질문 찾기)에 쓰므로 HNSW 인덱스를 둔다.
-- 시드 질문의 embedding은 비워 두고 앱 기동 시 채운다(QuestionEmbeddingBackfill).
-- created_by는 회원(app_user)을 FK로 묶지 않는다(다른 컨텍스트, 회원 삭제와 무관하게 질문 유지).

CREATE TABLE qb_question (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES qb_category (id),
    text        VARCHAR(300) NOT NULL,
    level       VARCHAR(10)  NOT NULL,           -- NEW | JUNIOR | MIDDLE | SENIOR
    intent      VARCHAR(200),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    seeded      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  BIGINT,
    embedding   vector(1536),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_qb_question_category ON qb_question (category_id, active);
CREATE INDEX ix_qb_question_embedding ON qb_question USING hnsw (embedding vector_cosine_ops);

CREATE TABLE qb_question_job_role (
    question_id BIGINT      NOT NULL REFERENCES qb_question (id) ON DELETE CASCADE,
    job_role    VARCHAR(20) NOT NULL,            -- BACKEND | FRONTEND | FULLSTACK | MOBILE | DATA | DEVOPS | COMMON
    PRIMARY KEY (question_id, job_role)
);

CREATE TABLE qb_question_tag (
    question_id BIGINT NOT NULL REFERENCES qb_question (id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES qb_tag (id),
    PRIMARY KEY (question_id, tag_id)
);
CREATE INDEX ix_qb_question_tag_tag ON qb_question_tag (tag_id);

-- 예시 질문: 소분류마다 2개
WITH seed (top, sub, text, level, roles) AS (
    VALUES
    ('Language', 'Java', 'Java에서 equals와 hashCode를 함께 재정의해야 하는 이유를 설명해 주세요.', 'JUNIOR', ARRAY['BACKEND']),
    ('Language', 'Java', 'JVM의 가비지 컬렉션이 어떻게 동작하는지, 겪어 본 GC 문제가 있다면 함께 말해 주세요.', 'MIDDLE', ARRAY['BACKEND']),
    ('Language', 'Python', '파이썬의 GIL이 무엇이고 멀티스레딩에 어떤 영향을 주나요?', 'JUNIOR', ARRAY['BACKEND', 'DATA']),
    ('Language', 'Python', '제너레이터를 쓰면 좋은 상황을 예를 들어 설명해 주세요.', 'NEW', ARRAY['BACKEND', 'DATA']),
    ('Language', 'JavaScript/TypeScript', '이벤트 루프와 마이크로태스크 큐의 동작 순서를 설명해 주세요.', 'JUNIOR', ARRAY['FRONTEND', 'FULLSTACK']),
    ('Language', 'JavaScript/TypeScript', 'TypeScript에서 any 대신 unknown을 쓰는 이유는 무엇인가요?', 'NEW', ARRAY['FRONTEND', 'FULLSTACK']),
    ('Language', 'Kotlin', '코틀린의 널 안정성이 자바와 어떻게 다른지 설명해 주세요.', 'NEW', ARRAY['BACKEND', 'MOBILE']),
    ('Language', 'Kotlin', '코루틴과 스레드의 차이, 그리고 구조화된 동시성이 무엇인지 설명해 주세요.', 'MIDDLE', ARRAY['BACKEND', 'MOBILE']),
    ('Language', 'Go', '고루틴과 채널로 동시성을 다룰 때 주의할 점은 무엇인가요?', 'JUNIOR', ARRAY['BACKEND', 'DEVOPS']),
    ('Language', 'Go', 'Go에서 에러를 값으로 다루는 방식의 장단점을 말해 주세요.', 'NEW', ARRAY['BACKEND']),
    ('Framework', 'Spring', '스프링의 트랜잭션 전파 옵션 중 REQUIRES_NEW를 써 본 상황을 설명해 주세요.', 'MIDDLE', ARRAY['BACKEND']),
    ('Framework', 'Spring', 'JPA에서 N+1 문제가 생기는 이유와 해결 방법을 설명해 주세요.', 'JUNIOR', ARRAY['BACKEND']),
    ('Framework', 'React', 'useEffect의 의존성 배열을 잘못 쓰면 어떤 문제가 생기나요?', 'NEW', ARRAY['FRONTEND']),
    ('Framework', 'React', '리렌더링을 줄이기 위해 어떤 방법을 써 봤는지 설명해 주세요.', 'JUNIOR', ARRAY['FRONTEND']),
    ('Framework', 'Next.js', '서버 컴포넌트와 클라이언트 컴포넌트를 나누는 기준은 무엇인가요?', 'JUNIOR', ARRAY['FRONTEND', 'FULLSTACK']),
    ('Framework', 'Next.js', 'SSR, SSG, ISR을 어떤 상황에서 각각 고르겠습니까?', 'MIDDLE', ARRAY['FRONTEND', 'FULLSTACK']),
    ('Framework', 'Node.js', 'Node.js에서 CPU를 많이 쓰는 작업을 처리할 때 생기는 문제와 해결 방법은요?', 'MIDDLE', ARRAY['BACKEND', 'FULLSTACK']),
    ('Framework', 'Node.js', '미들웨어 기반 요청 처리 흐름을 설명해 주세요.', 'NEW', ARRAY['BACKEND', 'FULLSTACK']),
    ('CS', '자료구조', '해시 테이블에서 충돌이 생겼을 때 해결하는 방법을 설명해 주세요.', 'NEW', ARRAY['COMMON']),
    ('CS', '자료구조', '배열과 연결 리스트를 각각 어떤 상황에서 쓰는 게 좋을까요?', 'NEW', ARRAY['COMMON']),
    ('CS', '알고리즘', '시간 복잡도를 줄이기 위해 알고리즘을 바꿔 본 경험을 말해 주세요.', 'JUNIOR', ARRAY['COMMON']),
    ('CS', '알고리즘', '정렬 알고리즘 중 하나를 골라 동작과 시간 복잡도를 설명해 주세요.', 'NEW', ARRAY['COMMON']),
    ('CS', '데이터베이스', '인덱스가 조회를 빠르게 하는 원리와 인덱스를 남발하면 생기는 문제를 설명해 주세요.', 'JUNIOR', ARRAY['BACKEND', 'DATA']),
    ('CS', '데이터베이스', '트랜잭션 격리 수준별로 생길 수 있는 문제를 설명해 주세요.', 'MIDDLE', ARRAY['BACKEND', 'DATA']),
    ('CS', '네트워크', 'TCP와 UDP의 차이를 설명하고, 각각 어디에 쓰이는지 말해 주세요.', 'NEW', ARRAY['COMMON']),
    ('CS', '네트워크', '브라우저에 주소를 입력했을 때 화면이 뜨기까지의 과정을 설명해 주세요.', 'JUNIOR', ARRAY['COMMON']),
    ('CS', '운영체제', '프로세스와 스레드의 차이를 설명해 주세요.', 'NEW', ARRAY['COMMON']),
    ('CS', '운영체제', '데드락이 생기는 조건과 막는 방법을 설명해 주세요.', 'JUNIOR', ARRAY['COMMON']),
    ('CS', '보안', 'XSS와 CSRF의 차이와 각각을 막는 방법을 설명해 주세요.', 'JUNIOR', ARRAY['FRONTEND', 'BACKEND']),
    ('CS', '보안', '비밀번호를 안전하게 저장하는 방법을 설명해 주세요.', 'NEW', ARRAY['BACKEND']),
    ('설계·인프라', '시스템 설계', '트래픽이 10배 늘어난다면 지금 시스템에서 무엇부터 바꾸겠습니까?', 'MIDDLE', ARRAY['BACKEND', 'DEVOPS']),
    ('설계·인프라', '시스템 설계', '캐시를 도입할 때 데이터 정합성을 어떻게 지키겠습니까?', 'MIDDLE', ARRAY['BACKEND']),
    ('설계·인프라', '클라우드', '오토스케일링을 설정할 때 어떤 지표를 기준으로 삼겠습니까?', 'MIDDLE', ARRAY['DEVOPS', 'BACKEND']),
    ('설계·인프라', '클라우드', '서버리스 구조의 장단점을 설명해 주세요.', 'JUNIOR', ARRAY['DEVOPS', 'BACKEND']),
    ('설계·인프라', 'DevOps', 'CI/CD 파이프라인을 만들 때 가장 신경 쓴 부분은 무엇인가요?', 'JUNIOR', ARRAY['DEVOPS']),
    ('설계·인프라', 'DevOps', '무중단 배포 방식 중 하나를 골라 동작과 장단점을 설명해 주세요.', 'MIDDLE', ARRAY['DEVOPS', 'BACKEND']),
    ('인성', '자기소개·동기', '1분 동안 자기소개를 해 주세요.', 'NEW', ARRAY['COMMON']),
    ('인성', '자기소개·동기', '우리 회사에 지원한 이유는 무엇인가요?', 'NEW', ARRAY['COMMON']),
    ('인성', '협업', '다른 직군과 협업하면서 가장 기억에 남는 경험을 말해 주세요.', 'NEW', ARRAY['COMMON']),
    ('인성', '협업', '팀에서 맡은 역할 밖의 일을 해결해 본 경험이 있나요?', 'JUNIOR', ARRAY['COMMON']),
    ('인성', '갈등', '팀원과 의견이 크게 달랐던 경험과 어떻게 풀었는지 말해 주세요.', 'NEW', ARRAY['COMMON']),
    ('인성', '갈등', '코드 리뷰에서 받아들이기 어려운 피드백을 받았을 때 어떻게 했나요?', 'JUNIOR', ARRAY['COMMON']),
    ('인성', '실패·성장', '실패했던 프로젝트와 그 경험에서 배운 점을 말해 주세요.', 'NEW', ARRAY['COMMON']),
    ('인성', '실패·성장', '최근에 새로 배운 기술과 그것을 배운 방법을 말해 주세요.', 'NEW', ARRAY['COMMON']),
    ('인성', '가치관', '좋은 개발자란 어떤 사람이라고 생각하나요?', 'NEW', ARRAY['COMMON']),
    ('인성', '가치관', '일정과 품질이 부딪힐 때 무엇을 기준으로 판단하나요?', 'JUNIOR', ARRAY['COMMON'])
),
inserted AS (
    INSERT INTO qb_question (category_id, text, level, seeded)
    SELECT c.id, s.text, s.level, TRUE
    FROM seed s
    JOIN qb_category p ON p.parent_id IS NULL AND p.name = s.top
    JOIN qb_category c ON c.parent_id = p.id AND c.name = s.sub
    RETURNING id, text
)
INSERT INTO qb_question_job_role (question_id, job_role)
SELECT i.id, unnest(s.roles)
FROM inserted i
JOIN seed s ON s.text = i.text;
