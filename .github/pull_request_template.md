## 요약
<!-- 무엇을, 왜 바꿨는지 2~3문장. 관련 계획: docs/exec-plans/... / ADR: docs/adr/... -->

## 변경 유형
- [ ] feat 새 기능
- [ ] fix 버그 수정
- [ ] refactor 동작 변화 없는 구조 개선
- [ ] docs 문서
- [ ] test 테스트
- [ ] build / ci / chore 빌드·CI·설정

## 변경 내용
<!-- 커밋 단위로 짧게. 리뷰어가 어디부터 보면 되는지 -->
-

## 영향 범위
- [ ] Backend (`backend/`)
- [ ] Frontend (`frontend/`)
- [ ] DB 스키마 (Flyway 새 파일: `V?__...sql`)
- [ ] API 계약 변경 (`pnpm gen:api` 반영 여부: )
- [ ] AI 프롬프트 (새 버전 파일 + `fake/` 샘플)
- [ ] 인프라 / CI / 하네스 (`.github/`, `.claude/`, `scripts/`)

## 검증
- [ ] `bash scripts/verify.sh` 통과
- [ ] 화면 변경 시 375px / 1280px 스크린샷 확인 + design-reviewer 위반 없음
- [ ] 새 동작에 테스트 추가 (또는 테스트가 불필요한 이유: )

<!-- 직접 확인한 방법(명령어, 클릭 경로)과 결과 -->

## 스크린샷
<!-- 화면 변경이 있을 때만 -->

## 리뷰 포인트 / 남은 일
<!-- 확신이 없는 부분, 의도적으로 미룬 것, 후속 작업 -->
-

## 체크리스트
- [ ] 비밀값·`.env`·개인 데이터가 포함되지 않음
- [ ] 관련 문서(docs/, CLAUDE.md) 갱신
- [ ] 커밋 메시지가 Conventional Commits 형식 (`feat:`, `fix:`, `docs:` …)
