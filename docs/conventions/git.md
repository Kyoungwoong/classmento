# Git · PR · 작업 절차

## 한 사이클 (요청 하나 = 브랜치 하나 = PR 하나)
```
1. 계획      사용자와 대화로 계획 확정 → docs/exec-plans/active/<날짜>-<slug>.md
2. 브랜치    git switch main && git pull → git switch -c <type>/<slug>
3. 개발      기능 단위로 잘게 커밋 (개발 = 메인 세션)
4. 검증      bash scripts/verify.sh 통과 (+ 화면이면 ui-screen 스킬)
5. PR        git push -u → gh pr create (템플릿 채움)
6. 리뷰      pr-reviewer 서브에이전트가 PR diff를 독립적으로 리뷰 → PR에 코멘트
7. 수정      REQUEST_CHANGES면 개발이 새 커밋으로 반영 → push → 6으로 (최대 3회)
8. 머지      CI ci-ok 통과 + 리뷰 APPROVE → 마스터가 merge commit으로 머지, 브랜치 삭제
9. 정리      git switch main && git pull, study-log 기록
```
이 절차는 `ship` 스킬에 단계별로 정리돼 있다. 기능 개발은 `feature` 스킬로 시작하고, 끝나면 `ship`으로 이어간다.

## 역할
| 역할 | 누가 | 하는 일 |
|---|---|---|
| 마스터 | 메인 세션 | 계획, 브랜치·PR 관리, 리뷰 결과 판단, 머지 |
| 개발 | 메인 세션 (큰 작업이면 개발 서브에이전트에 나눠 맡김) | 구현, 커밋, 리뷰 반영 |
| 리뷰 | `pr-reviewer` 서브에이전트 (매번 새 컨텍스트) | 개발 과정의 맥락 없이 PR diff만 보고 판단. 코드를 고치지 않음 |

리뷰어를 별도 컨텍스트로 두는 이유: 작성자의 의도를 모르는 상태여야 "작성자가 당연하다고 넘긴 것"을 잡을 수 있다.

## 브랜치 이름
`<type>/<짧은-영문-slug>` — 예: `feat/question-bank`, `fix/resume-upload-413`, `ci/docker-cache`, `docs/interview-modes`

## 커밋
- **Conventional Commits**: `<type>(<scope>)?: <한국어 요약>`
  - type: `feat` `fix` `refactor` `test` `docs` `build` `ci` `chore` `perf`
  - scope 예: `backend`, `frontend`, `resume`, `auth`, `ci`
- **잘게**: 커밋 하나 = 리뷰어가 한 번에 이해할 수 있는 논리적 변경 하나.
  - 순서 예: 스키마(V2) → 엔티티·리포지토리 → 서비스+테스트 → 컨트롤러+테스트 → `gen:api` → 화면 → 문서
  - 커밋마다 컴파일은 돼야 한다. 테스트는 같은 커밋이나 바로 다음 커밋에 넣는다.
- 본문에는 "왜"를 쓴다. 끝에 `Co-Authored-By` 줄을 붙인다(AI 작성 시).
- 리뷰를 반영할 때는 **새 커밋**(`fix: 리뷰 반영 - ...`)으로 한다. 이미 푸시한 커밋을 amend하거나 force push하지 않는다.

## PR
- 제목: 대표 커밋과 같은 형식(`feat: 질문 은행 관리`).
- 본문: `.github/pull_request_template.md`의 모든 칸을 채운다. 해당 없는 칸은 "해당 없음"이라고 쓴다.
- 크기: diff 800줄 이하를 권장한다(생성물 `schema.d.ts`와 lock 파일 제외). 넘으면 PR을 나눈다.

## 리뷰 판정
- **pr-reviewer**가 PR에 코멘트로 남긴다. 같은 계정이라 GitHub의 "Approve" 기능은 쓸 수 없어서 코멘트 마지막 줄에 판정을 쓴다.
  - `VERDICT: APPROVE` — 머지 가능
  - `VERDICT: REQUEST_CHANGES` — [필수] 항목이 하나 이상 있음
- [필수]는 버그, 보안, 규칙 위반, 테스트 누락이다. [권장]과 [질문]은 머지를 막지 않는다. 대신 후속 작업 메모로 남긴다.
- 3번 반영해도 APPROVE가 안 나오면 멈추고 사용자에게 묻는다.

## 머지
- 조건: `ci-ok` 성공 **그리고** 마지막 리뷰가 `VERDICT: APPROVE`
- 방식: `gh pr merge --merge --delete-branch`. merge commit으로 잘게 나눈 커밋을 보존한다. squash는 쓰지 않는다.
- 머지 후: `git switch main && git pull --ff-only`, 로컬 브랜치를 정리하고 study-log를 남긴다.
