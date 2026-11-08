---
name: ship
description: classmento 작업을 브랜치 → 잘게 커밋 → verify → 푸시 → PR(템플릿) → pr-reviewer 리뷰 → 수정 반복 → 머지 → main pull 순서로 마무리할 때 사용. 사용자와 계획을 확정한 뒤 구현을 시작할 때, 또는 "PR 올려줘/머지해줘/작업 절차대로 진행해줘"라고 할 때 사용한다.
---

# Ship: 계획 이후 ~ 머지까지

규칙 전문: `docs/conventions/git.md`. 각 단계에서 실패하면 다음 단계로 넘어가지 않는다.

## 0. 시작 조건
- 사용자와 계획이 확정돼 있다(계획 모드 승인 또는 대화로 합의).
- 계획을 `docs/exec-plans/active/<YYYY-MM-DD>-<slug>.md`에 저장한다(템플릿: `docs/exec-plans/README.md`).

## 1. 브랜치
```bash
git status --short          # 남은 변경이 있으면 사용자에게 먼저 묻는다
git switch main && git pull --ff-only
git switch -c <type>/<slug>
```

## 2. 개발 + 잘게 커밋
- 논리 단위가 끝날 때마다 커밋한다. 커밋 순서 예시는 git.md에 있다.
- `git add <파일들>`로 해당 단위만 담는다(`git add -A`는 단위가 하나일 때만).
- 메시지: `type(scope): 한국어 요약` + 본문(왜) + `Co-Authored-By` 줄.
- 백엔드 API를 바꿨으면 같은 PR 안에서 `pnpm gen:api`를 실행하고 타입을 커밋한다.

## 3. 검증
- `bash scripts/verify.sh` 전부 통과. 화면이 바뀌었으면 `ui-screen` 스킬의 스크린샷과 design-reviewer 확인까지.
- 계획 파일의 체크박스와 결정 로그를 갱신하고 `completed/`로 옮긴다(커밋 포함).

## 4. 푸시 + PR
```bash
git push -u origin HEAD
gh pr create --base main --title "<type>: <요약>" --body-file <임시파일>
```
- 본문은 `.github/pull_request_template.md`의 모든 섹션을 채운다(검증 결과에는 실제로 돌린 명령과 결과를 적는다).
- 만든 뒤 PR 번호와 URL을 기록한다.

## 5. CI 확인
```bash
gh pr checks <번호> --watch --interval 30     # ci-ok 결과까지 (백그라운드로 실행)
```
- 실패하면 `gh run view <run-id> --log-failed`로 원인을 보고, 고친 커밋을 push한 뒤 다시 확인한다.

## 6. 리뷰 (독립 서브에이전트)
- `pr-reviewer` 서브에이전트를 실행한다. 프롬프트에는 **PR 번호와 저장소만** 준다. 내가 의도한 바나 작업 요약은 주지 않는다(편향 방지).
- 리뷰어가 PR에 코멘트를 남기고 판정(`VERDICT: APPROVE | REQUEST_CHANGES`)을 돌려준다.

## 7. 수정 반복 (최대 3회)
- [필수] 항목을 하나씩 반영한다. 각 반영은 새 커밋(`fix: 리뷰 반영 - <요지>`)으로 하고, verify → push.
- 동의하지 않는 항목은 PR 코멘트로 근거를 남긴다(`gh pr comment`).
- 다시 6번을 실행한다(새 리뷰어). 3회째에도 REQUEST_CHANGES면 멈추고 사용자에게 보고한다.

## 8. 머지 (마스터)
- 조건 확인: `gh pr checks <번호>`가 전부 통과(`ci-ok` 포함)하고, 마지막 판정이 APPROVE.
```bash
gh pr merge <번호> --merge --delete-branch
git switch main && git pull --ff-only
git branch -d <브랜치>    # 남아 있으면
```

## 9. 기록
- `study-log/<날짜>/NN-<slug>.md` + 목차(CLAUDE.md "작업 기록" 규칙). PR 번호와 리뷰 반영 내역을 포함한다.
- 사용자에게 보고: PR 링크, 커밋 수, 리뷰 라운드 수와 반영 요지, 남은 후속 작업.
