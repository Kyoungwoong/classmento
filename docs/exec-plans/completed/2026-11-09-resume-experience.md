# 이력서 경험(ResumeExperience) 단위 분리

## 목표
- 구조화된 이력서의 경력·프로젝트를 경험 단위로 저장(요약 + 임베딩)하고 조회 API를 연다.
- 경험 기반 면접(ADR 0005)의 재료를 만든다.

## 범위
- 포함(PR A):
  - 도메인 `ResumeExperience` + `ExperienceFactory`(TDD)
  - 프롬프트 v2(summary)
  - V2 마이그레이션, JDBC 저장소(kNN)
  - 처리 흐름 연결, `GET /api/resumes/{id}/experiences`
- 포함(PR #6): 결과 화면의 경험 배너
- 제외: 경험 선택 후 면접 진행(면접 기능), 경험 직접 편집

## 단계
- [x] ADR 0005
- [x] ResumeExperience 규칙 red/green
- [x] ExperienceFactory red/green
- [x] resume-parse.v2 + 가짜 응답
- [x] V2 + JdbcExperienceRepository(통합 테스트)
- [x] ResumeProcessor·updateParsed 연결
- [x] 경험 목록 API red/green + gen:api
- [x] 문서

## 결정 로그
- 2026-11-09: 클래스 이름은 `ResumeExperience`로 한다. `ParsedResume.Experience`(경력 항목)와 헷갈리지 않게 하기 위해서다.
- 2026-11-09: 경험 저장소는 JPA 엔티티가 아니라 JDBC로 만든다. 벡터와 jsonb를 같이 다루고, 항상 이력서 단위로 통째 교체하기 때문이다.
- 2026-11-09 (리뷰 1):
  - 회사명 100자·기간 50자로 말줄임
  - 기술 연결은 단어 경계로
  - 임베딩은 트랜잭션 밖에서 하고, 저장과 상태 전이는 한 트랜잭션으로
  - PARSED가 아닌 이력서는 빈 경험 목록
  - 이력서별 kNN은 정확 정렬(V3에서 HNSW 제거)

## 결과
- PR #4(백엔드): 경험 도메인·팩토리 TDD, 프롬프트 v2, V2·V3 마이그레이션, 경험 API. 리뷰 2라운드.
- PR #5(버그): 옛 세션 쿠키로 모든 요청 500 → 읽을 수 없는 세션 속성은 무시(비로그인). 리뷰에서 승인.
- PR #6(화면): 결과 화면 경험 배너, 빈 상태·에러·다시 나누기, Chip sm/surface. design-reviewer 반영.
- 남은 일:
  - 경험 선택 → 면접(면접 기능)
  - ~~중복 인덱스 `ix_resume_chunk_resume` 정리~~ → PR #7(V4)
  - ~~동시 저장 잠금~~ → PR #7. `@Version`(충돌 시 409) 대신 **비관적 잠금**(`PESSIMISTIC_WRITE`)을 골랐다. 경험 DELETE/INSERT가 JDBC로 먼저 실행돼 낙관적 버전 검사보다 앞서 unique 충돌이 나기 때문이다.
  - ~~세션 경고 로그 빈도~~ → PR #7(DEBUG)
