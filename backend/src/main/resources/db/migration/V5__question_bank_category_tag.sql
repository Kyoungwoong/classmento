-- 질문 은행: 카테고리(대분류 → 소분류 2단계)와 태그.
-- seeded = 기본 데이터(시스템 시드). 통합 테스트는 seeded가 아닌 행만 지운다.

CREATE TABLE qb_category (
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT       REFERENCES qb_category (id),   -- 대분류는 NULL
    name       VARCHAR(30)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    seeded     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- 같은 부모 아래 이름 중복 금지(공백 제거 + 소문자). 도메인 검사의 최종 안전장치.
CREATE UNIQUE INDEX uq_qb_category_sibling_name
    ON qb_category (COALESCE(parent_id, 0), lower(regexp_replace(name, '\s+', '', 'g')));
CREATE INDEX ix_qb_category_parent ON qb_category (parent_id, sort_order);

CREATE TABLE qb_tag (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(20)  NOT NULL,
    name_key   VARCHAR(20)  NOT NULL UNIQUE,              -- 대소문자 무시 중복 판단
    seeded     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 기본 카테고리 (ADR 0005: Language · Framework · CS · 인성 + 설계·인프라)
INSERT INTO qb_category (name, sort_order, seeded) VALUES
    ('Language', 0, TRUE), ('Framework', 1, TRUE), ('CS', 2, TRUE), ('설계·인프라', 3, TRUE), ('인성', 4, TRUE);

INSERT INTO qb_category (parent_id, name, sort_order, seeded)
SELECT p.id, c.name, c.ord, TRUE
FROM qb_category p
JOIN (VALUES
    ('Language', 'Java', 0), ('Language', 'Python', 1), ('Language', 'JavaScript/TypeScript', 2),
    ('Language', 'Kotlin', 3), ('Language', 'Go', 4),
    ('Framework', 'Spring', 0), ('Framework', 'React', 1), ('Framework', 'Next.js', 2), ('Framework', 'Node.js', 3),
    ('CS', '자료구조', 0), ('CS', '알고리즘', 1), ('CS', '데이터베이스', 2), ('CS', '네트워크', 3),
    ('CS', '운영체제', 4), ('CS', '보안', 5),
    ('설계·인프라', '시스템 설계', 0), ('설계·인프라', '클라우드', 1), ('설계·인프라', 'DevOps', 2),
    ('인성', '자기소개·동기', 0), ('인성', '협업', 1), ('인성', '갈등', 2), ('인성', '실패·성장', 3), ('인성', '가치관', 4)
) AS c(parent, name, ord) ON p.name = c.parent AND p.parent_id IS NULL;
