# 실행 계획 (exec-plans)

여러 파일·여러 단계에 걸친 기능은 코드 전에 계획 파일을 만든다. `feature` 스킬이 이 템플릿을 쓴다.

- 진행 중: `active/YYYY-MM-DD-<slug>.md`
- 완료: `completed/`로 옮기고 맨 아래 "결과" 채우기

## 템플릿
```markdown
# <기능 이름>

## 목표
사용자가 무엇을 할 수 있게 되나 (한두 문장). 관련 화면: docs/design/screens.md#...

## 범위
- 포함:
- 제외:

## 설계
- API: METHOD /api/... 요청/응답 요약 (docs/api/README.md 표에도 추가)
- 백엔드: 패키지/클래스
- 프론트: 라우트/컴포넌트
- AI: 사용하는 프롬프트와 버전

## 단계
- [ ] 1. 백엔드 API + 테스트
- [ ] 2. `pnpm gen:api`
- [ ] 3. 화면 (ui-screen 스킬)
- [ ] 4. verify + 스크린샷 리뷰

## 결정 로그
- YYYY-MM-DD: 무엇을 왜 바꿨나

## 결과
(완료 시) 실제로 된 것, 남은 것
```
