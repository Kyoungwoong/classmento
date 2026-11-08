# CI / CD · 배포

## 구성
| 파일 | 역할 | 상태 |
|---|---|---|
| `.github/workflows/ci.yml` | PR·main 푸시마다 프론트·백엔드 검사 + Docker 빌드(푸시 없음) | **동작 중** |
| `.github/workflows/docker-image.yml` | 재사용 워크플로: 서비스 하나 빌드 / (선택) ECR 푸시 | CI·CD가 공유 |
| `.github/workflows/cd.yml` | ECR 푸시 + 배포 | **골격만** (`vars.CD_ENABLED`로 켬) |
| `backend/Dockerfile` | Gradle 빌드 → JRE 17 런타임, `prod` 프로필 | |
| `frontend/Dockerfile` | Next standalone → node 24 alpine | |

## CI 잡
```
changes(PR이면 바뀐 폴더 확인)
 ├ frontend : format → lint → typecheck → test → scripts/check-design.sh → build
 ├ backend  : spotlessCheck → test   (서비스 컨테이너 pgvector/pgvector:pg17 @5433)
 └ docker   : backend/frontend 이미지 빌드 (push=false, GHA 캐시)
ci-ok       : 위 결과 종합 → 브랜치 보호의 필수 체크로 이것 하나만 지정
```
- 로컬의 `bash scripts/verify.sh`와 같은 검사다. 로컬에서 통과하면 CI도 통과해야 한다.
- 실패한 백엔드 테스트 리포트는 Actions 실행 페이지의 Artifacts(`backend-test-report`)에서 받는다.

## 권장 GitHub 설정 (저장소 관리자가 직접)
- **Branch protection (main):** PR 필수, 필수 체크 `ci-ok`, 브랜치 최신화 필수, force push 금지
- **Merge 방식:** "Create a merge commit"만 허용. 잘게 나눈 커밋을 보존하고 PR 단위로 묶기 위해서다. 운영 규칙: [conventions/git.md](../conventions/git.md)
- **Automatically delete head branches:** 켜기

## CD 켜기 (AWS)
1. **ECR 저장소:** `classmento-backend`, `classmento-frontend`
2. **OIDC 역할:** GitHub OIDC 공급자(`token.actions.githubusercontent.com`)를 신뢰하는 IAM 역할을 만든다.
   - 조건: `repo:Kyoungwoong/classmento:ref:refs/tags/v*`와 `environment:staging|production`
   - 권한: ECR push(+ 이후 ECS 배포)
3. **저장소 Variables:** `AWS_ROLE_ARN`, `AWS_REGION`(기본 ap-northeast-2), `BACKEND_INTERNAL_URL`(프론트 이미지의 /api 프록시 대상), `CD_ENABLED=true`
4. **Environments:** `staging`, `production` 생성. production에는 승인자를 지정한다.
5. **배포 대상:** 확정되면 `cd.yml`의 `deploy` 잡 TODO를 채운다. ECS Fargate라면 task definition 렌더 → 배포 액션을 쓴다.
6. **운영 비밀값:** `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `CLOVA_*`, `DB_*`, 소셜 키
   - 이미지에 넣지 않는다. 실행 환경(ECS 태스크 정의의 Secrets Manager/SSM 참조)에서 주입한다.

장기 AWS 액세스 키를 GitHub Secrets에 넣지 않는다(OIDC만 쓴다).

## 이미지 메모
- **backend:** `SPRING_PROFILES_ACTIVE=prod`이므로 `DB_URL/DB_USERNAME/DB_PASSWORD`가 필요하다. Flyway가 기동할 때 마이그레이션을 적용한다.
  - 업로드 파일은 `/app/data/uploads`에 쓴다. 컨테이너는 휘발성이므로 운영 전에 `FileStorage`의 S3 구현이 필요하다.
- **frontend:** `/api` 프록시 대상(`BACKEND_URL`)이 **빌드 시점에 고정**된다. 환경마다 값이 다르면 이미지를 따로 빌드한다.
- **로컬에서 이미지 빌드(WSL):**
  ```bash
  wsl docker build -t classmento-backend ./backend
  ```
  ```bash
  wsl docker build --build-arg BACKEND_URL=http://backend:8080 -t classmento-frontend ./frontend
  ```
