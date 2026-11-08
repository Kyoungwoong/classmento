# DDD 구조 도입 + 기존 코드(identity, resume) 리팩터

## 목표
- 백엔드를 **바운디드 컨텍스트 × 계층(domain / application / infrastructure / presentation)** 구조로 바꾼다.
- 도메인 규칙은 엔티티와 값 객체 안에 둔다.
- 계층 의존 방향을 **ArchUnit 테스트**로 강제해, 어기면 CI가 실패하게 한다.
- 이후 기능은 이 구조와 TDD(테스트 먼저)로 만든다.

## 범위
- **포함:**
  - DDD·TDD 규칙 문서
  - ArchUnit 규칙
  - `auth`·`admin` → `identity` 컨텍스트
  - `resume` 컨텍스트 계층 분리
  - 도메인 규칙: 이메일 값 객체, 이력서 상태 전이 불변식
- **제외:**
  - 공용 AI·스토리지 포트(`llm`, `embedding`, `speech`, `storage`)의 위치 이동 → 이번에는 "플랫폼 포트"로 문서화만 한다
  - API 동작 변경 → 외부 계약은 유지한다. 단, 잘못된 상태 전이 응답만 409 `INVALID_STATE`로 정리한다.

## 설계
```
com.classmento
  common/         공용 커널: web(에러·CORS·헬스), security(AuthUser), domain(DomainException)
  identity/       회원·인증·관리자
    domain/         AppUser(애그리거트), Email(값 객체), AppUserRepository(인터페이스)
    application/    SignupService, MemberQueryService, AdminSeeder
    infrastructure/ persistence(JPA 구현), security(SecurityConfig, 소셜, UserDetails)
    presentation/   AuthController, AdminController, DTO
  resume/
    domain/         Resume(애그리거트, 상태 전이), ResumeFile, ResumeFileType, ParsedResume, TextChunker,
                    ResumeRepository, ResumeChunkRepository(인터페이스)
    application/    ResumeService(업로드·조회·수정), ResumeProcessor, ResumeParser, TextExtractor(포트)
    infrastructure/ JPA·JDBC(pgvector) 구현, PDF/DOCX 추출기, 실행기 설정
    presentation/   ResumeController, DTO
  llm/ embedding/ speech/ storage/   플랫폼 포트 + 어댑터 (여러 컨텍스트가 공유)
```
의존 규칙 (ArchUnit):
- domain은 같은 컨텍스트의 다른 계층에 의존하지 않는다. Spring에도 의존하지 않는다(JPA 애너테이션은 허용).
- application은 infrastructure·presentation에 의존하지 않는다.
- 컨텍스트끼리는 직접 의존하지 않는다(`common`을 통해서만).

## 단계 (커밋 단위)
- [ ] docs: DDD·TDD 규칙
- [ ] build: ArchUnit
- [ ] test(arch): 계층 규칙 (red — 현재 구조에서 실패)
- [ ] refactor(common): AuthUser → common.security, DomainException
- [ ] refactor(identity): 패키지 계층 분리 + 리포지토리 인터페이스/JPA 구현 분리
- [ ] test(identity): Email 값 객체 (red) → feat(identity): Email 구현·적용 (green)
- [ ] refactor(resume): 계층 분리 + 포트(TextExtractor, 리포지토리) 분리
- [ ] test(resume): 상태 전이 불변식 (red) → feat(resume): 불변식 구현 + 409 매핑 (green)
- [ ] arch 테스트 green 확인, docs/architecture.md 갱신

## 결정 로그
- 2026-11-08: 도메인 엔티티에 JPA 애너테이션 허용(실용적 DDD).
  - 도메인 모델과 영속 모델을 따로 두면 매핑 코드가 두 배가 된다. 지금 규모에서는 이득보다 비용이 크다.
  - Spring Data 리포지토리 인터페이스는 infrastructure에 둔다. domain에는 순수 Java 리포지토리 인터페이스만 둔다.
- 2026-11-08: red 커밋도 **컴파일은 돼야 한다**. 새 API는 `UnsupportedOperationException`을 던지는 뼈대로 두고 테스트를 실패시킨다. PR head는 항상 green.
