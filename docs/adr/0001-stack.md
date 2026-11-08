# ADR 0001: 기술 스택과 AI 공급자

- 상태: 채택 (2026-11-08)

## 결정
| 영역 | 선택 |
|---|---|
| 프론트엔드 | Next.js 16 (App Router) + TypeScript + Tailwind v4, pnpm |
| 백엔드 | Spring Boot 4.1 + Java 17 + Gradle(Kotlin DSL) |
| DB | 로컬 H2 파일(Docker 없음), 운영 PostgreSQL(`prod` 프로필) |
| LLM | Claude API (`claude-sonnet-5-5` 기본) — 이력서 구조화, 질문 생성, 평가 |
| STT | NAVER CLOVA Speech — 한국어 인식 정확도, 국내 서비스 |
| API 계약 | springdoc OpenAPI → openapi-typescript로 FE 타입 생성 |
| AI 개발 하네스 | Claude Code 전용: CLAUDE.md 계층 + docs + `.claude/skills·agents·settings` + `.mcp.json` |

## 이유
- FE/BE 분리: 음성 업로드·AI 호출·평가 저장을 백엔드에 모아 키를 브라우저에 노출하지 않는다.
- Spring Boot: 팀 익숙도, 국내 기업형 구조. Java 17이 설치된 환경이라 toolchain 17 고정(Boot 4도 17 지원).
- 포트/어댑터: Claude·CLOVA를 인터페이스 뒤에 두어 공급자 교체와 테스트(Fake)를 쉽게.
- OpenAPI 생성 타입: FE/BE 계약 불일치를 컴파일 단계에서 잡는다.

## 결과
- 두 런타임(Node, JVM)을 함께 다뤄야 한다 → `scripts/verify.sh` 하나로 묶는다.
- CLOVA Speech의 입력 포맷(webm 지원 여부)은 실제 키로 확인 필요 → 첫 녹음 기능 작업에서 검증.
