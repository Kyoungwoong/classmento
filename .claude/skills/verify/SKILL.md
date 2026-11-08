---
name: verify
description: classmento 작업을 끝내기 전, 또는 사용자가 "검증/테스트/빌드 돌려줘"라고 할 때 사용. 프론트(format, lint, typecheck, test, 디자인 토큰 검사, build)와 백엔드(spotless, test)를 한 번에 확인하고 실패 원인을 정리한다.
---

# 검증

1. 루트에서 실행: `bash scripts/verify.sh` (프론트만 `--fe`, 백엔드만 `--be`). 백엔드 첫 실행은 몇 분 걸릴 수 있으니 타임아웃을 넉넉히.
2. 실패 시 단계별로:
   - **FE format** → `cd frontend && pnpm format`
   - **FE lint / typecheck** → 메시지대로 수정. API 타입 에러면 `pnpm gen:api`가 필요한지 확인.
   - **FE design** → 출력된 줄의 임의값을 토큰 클래스로 교체(`docs/design/design-system.md` 토큰 표).
   - **BE spotless** → `cd backend && ./gradlew spotlessApply`
   - **BE test** → `backend/build/reports/tests/test/index.html` 또는 `./gradlew test --info`에서 원인 확인.
3. 고친 뒤 다시 전체 실행. 통과하기 전에는 "완료"라고 보고하지 않는다.
4. 보고: 통과/실패 단계, 고친 내용, 남은 문제를 짧게.
