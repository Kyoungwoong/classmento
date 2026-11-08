# 회원·관리자 + PostgreSQL(pgvector) 전환 + 이력서 업로드

## 목표
- 사용자가 이메일로 가입·로그인한 뒤 이력서(PDF/DOCX)를 올리면, 경력·기술·프로젝트로 정리된 결과를 확인하고 기술 목록을 고칠 수 있다.
- 관리자는 회원 목록을 본다.
- 이력서는 청크와 벡터로 저장되어, 다음 기능(질문 생성 RAG)에서 kNN 검색할 수 있다.

## 범위
- **포함:**
  - PostgreSQL 17 + pgvector, Flyway
  - 세션 인증, CSRF, 소셜 로그인 구조(키 설정 시 활성)
  - 관리자 시드와 회원 목록
  - 이력서 업로드 → 추출 → 청크 → 임베딩 → 구조화, 상태 폴링, 기술 수정
  - 가짜 AI 모드
- **제외:**
  - 질문 은행 CRUD, JD 업로드, 크레딧(ADR 0003 메모만)
  - 경력·프로젝트 행 수정(바텀시트)
  - 비밀번호 찾기

## 설계
- API: [docs/api/README.md](../../api/README.md) 엔드포인트 표
- 백엔드: `auth`, `admin`, `embedding`, `storage`, `resume` 패키지 + `V1__init.sql`
- 프론트: `/login`, `/signup`, `/resume/upload`, `/resume/[id]`, `/admin`, `src/proxy.ts`, `next.config.ts` rewrites
- AI: `resume-parse.v1`(+ 가짜 응답 `fake/resume-parse.v1.json`), OpenAI `text-embedding-3-small`

## 단계
- [x] 1. docker-compose(pg17, :5433) + `scripts/db.sh`
- [x] 2. Flyway + 설정 전환
- [x] 3. 인증·관리자 + 테스트
- [x] 4. 이력서·임베딩·스토리지 + 테스트
- [x] 5. `pnpm gen:api`
- [x] 6. 화면 5개 + 브라우저에서 직접 흐름 확인
- [x] 7. 문서·ADR
- [x] 8. design-reviewer 지적 반영 + verify

## 결정 로그
- 2026-11-08: 사용자 요청으로 PostgreSQL 16 → **17**. WSL Docker를 쓰고, 기존 `opicer-postgres`(5432)와 겹치지 않게 **5433**으로 띄운다.
- 2026-11-08: Testcontainers 대신 실제 로컬 Postgres의 `classmento_test` DB를 쓴다(Windows 호스트에 Docker가 없음).
- 2026-11-08: JWT 대신 세션을 쓰고, Next rewrites로 같은 출처를 만든다 → ADR 0002.
- 2026-11-08: springdoc 생성 타입이 전부 optional로 나와 응답 record에 `@Schema(requiredProperties)`를 추가했다.
- 2026-11-08: Next 16 `cacheComponents` 모드에서 `useParams`/`useSearchParams`는 `<Suspense>`가 필요하다.

## 결과
- 백엔드 테스트 24개가 실제 Postgres에서 통과한다(가입/로그인/CSRF/관리자 권한/업로드 PDF·DOCX/남의 이력서 404/kNN 순서).
- 브라우저에서 가입 → 업로드 → 파싱 결과 → 기술 추가·저장 → 로그아웃 → 관리자 로그인 → 회원 목록까지 확인했다.
- 남은 일:
  - 실제 키(Anthropic, OpenAI)로 구조화·임베딩 품질 확인
  - 경력·프로젝트 수정 UI
  - 스캔 PDF(OCR) 미지원
