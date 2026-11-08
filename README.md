# classmento

이력서를 올리면 파싱하고, 이력서 기반 기술·인성 면접 질문을 만들고, 음성 답변을 녹음 → STT → AI 평가하는 모의면접 서비스.

## 구성
- `frontend/` — Next.js 16, Tailwind v4, pnpm
- `backend/` — Spring Boot 4.1, Java 17, Gradle
- `docs/` — 제품·아키텍처·디자인 시스템·API·AI 루브릭 문서
- `CLAUDE.md`, `.claude/`, `.mcp.json` — Claude Code 하네스(규칙, 스킬, 서브에이전트, 훅, MCP)

## 실행
```bash
# 환경변수 (.env.example 참고) — 키가 없어도 앱은 뜨고 AI 기능만 503
cd backend && ./gradlew bootRun     # http://localhost:8080 (Swagger: /swagger-ui.html)
cd frontend && pnpm install && pnpm dev   # http://localhost:3000
```

## 검증
```bash
bash scripts/verify.sh
```
