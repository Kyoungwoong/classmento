# ADR 0002: PostgreSQL 17 + pgvector, 세션 인증, OpenAI 임베딩

- 상태: 채택 (2026-11-08)
- 대체: ADR 0001의 "로컬 H2" 부분

## 배경
서비스의 핵심 기능이 벡터 검색(kNN)에 기대고 있다.
- 이력서 청크 검색(RAG)
- 질문 은행 검색
- 채용공고(JD) 매칭

H2에는 벡터 타입과 근접 검색이 없다. 또 회원·관리자가 필요하고, 나중에 크레딧 결제가 붙는다.

## 결정
| 영역 | 선택 |
|---|---|
| DB | PostgreSQL 17 + pgvector (`pgvector/pgvector:pg17`). 로컬은 WSL Docker에 띄우고 포트는 5433(다른 프로젝트 5432와 분리) |
| 스키마 | Flyway 마이그레이션(`db/migration/V*.sql`), `ddl-auto: validate` |
| 벡터 검색 | JdbcTemplate + `<=>`(코사인 거리) + HNSW 인덱스. JPA 엔티티로 매핑하지 않음 |
| 임베딩 | OpenAI `text-embedding-3-small`(1536차원). Claude에는 임베딩 API가 없음 |
| 인증 | Spring Security 세션(HttpOnly 쿠키). 세션은 `spring-session-jdbc`로 DB에 저장 |
| CSRF | 쿠키 방식(`XSRF-TOKEN` 쿠키 → `X-XSRF-TOKEN` 헤더), `csrf.spa()` |
| FE↔BE | Next.js `rewrites`로 `/api/*` 프록시 → 같은 출처라 CORS·쿠키 문제가 없음 |
| 소셜 | Google·Kakao OAuth2 클라이언트. 키가 있을 때만 등록 |
| 관리자 | `Role.ADMIN`. 계정은 `ADMIN_EMAIL`/`ADMIN_PASSWORD`로 시드, `/api/admin/**` 보호 |
| 키 없는 개발 | `classmento.ai.fake=true`(local 기본)면 가짜 LLM(고정 JSON)과 해시 임베딩 사용 |
| 테스트 DB | 실제 Postgres의 `classmento_test` DB. Windows에 Docker가 없어 Testcontainers는 쓰지 않음 |

## 이유
- **pgvector:** 관계형 데이터(회원·이력서·크레딧)와 벡터를 한 DB·한 트랜잭션에서 다룬다. 별도 벡터 DB를 운영하지 않아도 된다.
- **세션 vs JWT:** 같은 출처 구성이면 세션 쿠키가 가장 단순하고 안전하다(XSS로 토큰을 훔칠 수 없음, 로그아웃 즉시 무효화). 모바일 앱이 생기면 그때 토큰을 추가한다.
- **JdbcTemplate 벡터 쿼리:** SQL이 그대로 보여 학습·튜닝이 쉽다. Hibernate vector 모듈 의존도 없앤다.
- **가짜 AI 모드:** 키를 받기 전에도 화면까지 전체 흐름을 개발·검증할 수 있다. 로그에 경고가 남고, prod 기본값은 false다.

## 결과 / 주의
- 백엔드 테스트·실행 전에 DB가 떠 있어야 한다(`bash scripts/db.sh up`). `verify.sh`는 DB를 자동으로 올린다.
- 임베딩 차원(1536)은 스키마에 고정된다. 모델을 바꾸면 새 마이그레이션과 재임베딩이 필요하다.
- 소셜 로그인은 같은 이메일의 기존 계정과 자동으로 연결하지 않는다(계정 탈취 방지). 연결 기능은 나중에 따로 만든다.
- 백그라운드 실행(`gradlew bootRun`)을 멈춰도 java 프로세스가 남아 8080을 점유할 수 있다. 이때는 포트를 쓰는 프로세스를 종료한다.
