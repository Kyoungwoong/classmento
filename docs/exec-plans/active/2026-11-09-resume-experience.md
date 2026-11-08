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
- 포함(PR B): 결과 화면의 경험 배너
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
