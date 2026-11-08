# DDD · TDD 규칙 (백엔드)

## 1. 바운디드 컨텍스트
| 컨텍스트 | 패키지 | 책임 | 상태 |
|---|---|---|---|
| 회원 | `identity` | 가입·로그인·소셜·역할·관리자 | 구현 |
| 이력서 | `resume` | 업로드·추출·청크·구조화 | 구현 |
| 질문 은행 | `questionbank` | 관리자 질문 관리·검색 | 예정 |
| 면접 | `interview` | 세션(4유형)·질문 생성·꼬리질문·답변 | 예정 |
| 평가 | `evaluation` | 채점·리포트 | 예정 |
| 결제 | `billing` | 크레딧 원장·결제 | 예정 |
| 공용 커널 | `common` | 에러 응답, 현재 사용자(`AuthUser`), `DomainException`, 웹 설정 | — |
| 플랫폼 포트 | `llm` `embedding` `speech` `storage` | 외부 기술 포트 + 어댑터 (여러 컨텍스트가 공유) | — |

**컨텍스트끼리는 직접 import하지 않는다.**
- 필요한 정보는 ID(`Long userId`)나 `common` 타입으로 주고받는다.
- 다른 컨텍스트의 데이터가 필요하면 그쪽 application 서비스를 부르는 "공개 API"를 만든다. 이때는 문서에 남기고 ArchUnit에 예외로 등록한다.

## 2. 계층
```
<context>/
  domain/          애그리거트·엔티티·값 객체·도메인 서비스·리포지토리 "인터페이스"·도메인 예외
  application/     유스케이스(트랜잭션 경계), 외부 포트 인터페이스, 커맨드/결과 record
  infrastructure/  리포지토리 구현(JPA/JDBC), 외부 어댑터, 보안·설정
  presentation/    컨트롤러, 요청/응답 DTO
```
의존 방향: `presentation → application → domain ← infrastructure`
- **domain:** 다른 계층과 Spring(`org.springframework..`)에 의존하지 않는다.
  - 허용: `jakarta.persistence`, `org.hibernate.annotations`(실용적 선택: 영속 모델을 따로 두지 않음), Jackson·Swagger 애너테이션(값 객체 직렬화)
- **application:** infrastructure·presentation에 의존하지 않는다. Spring(`@Service`, `@Transactional`)은 허용한다.
- **infrastructure:** domain·application의 인터페이스를 구현한다.
- **presentation:** application을 호출한다. 엔티티를 응답으로 직접 내보내지 않는다(DTO로 변환).
  - 저장소 구현(`infrastructure.persistence`, Spring Data, JDBC)에 의존하지 않는다(ArchUnit).
  - 예외: 인증 배관(`identity.infrastructure.security`의 설정 record·`SecurityContextRepository` 등)은 컨트롤러가 참조할 수 있다. 로그인은 HTTP 세션과 맞물려 있어서다.

이 규칙은 `ArchitectureTest`(ArchUnit)가 검사한다. 어기면 `./gradlew test`가 실패한다.

## 3. 도메인 모델 작성법
- **규칙은 엔티티 안에.**
  - 상태를 바꾸는 메서드가 불변식을 검사하고, 어기면 `DomainException`을 던진다.
  - setter를 만들지 않고, 의미 있는 이름의 메서드를 쓴다: `markParsed`, `registerLocal`.
- **값 객체는 `record`.**
  - 생성 시점에 검증하고 정규화한다(예: `Email`은 소문자로 바꾸고 형식을 검사한다).
- **애그리거트 경계:**
  - 다른 애그리거트는 ID로만 참조한다(`Resume.userId`).
  - 한 트랜잭션에서는 애그리거트 하나만 바꾸는 것을 원칙으로 한다.
- **리포지토리:**
  - domain에는 필요한 메서드만 가진 인터페이스를 둔다.
  - infrastructure의 Spring Data 인터페이스가 그것을 함께 상속해 구현한다.
- **도메인 예외 → HTTP:**
  - `DomainException(code, message)`은 409로 바꾼다. 상태 규칙 위반이나 중복이 여기에 해당한다.
  - 입력 형식 오류는 presentation 검증(Bean Validation)에서 **400**으로 처리한다.
  - 같은 형식 규칙이 도메인에도 있으면, 정규식 같은 기준을 도메인 상수로 공유한다(예: `Email.INPUT_REGEX`). 그래야 입력이 409까지 가지 않는다.
  - `ApiException`(HTTP 상태를 직접 지정)은 application·presentation의 입력·조회 오류(400·404)에만 쓴다. 도메인에서는 쓰지 않는다.

## 4. TDD
**사이클:** 실패하는 테스트(red) → 통과시키는 최소 구현(green) → 정리(refactor)

| 계층 | 테스트 종류 | 도구 |
|---|---|---|
| domain | 순수 단위 테스트 (Spring 없음, 가장 많이) | JUnit + AssertJ |
| application | 유스케이스 단위 테스트, 포트는 가짜(`Fake*`) | JUnit |
| infrastructure | 실제 DB 통합 테스트 (pgvector 쿼리 등) | `IntegrationTest` |
| presentation | MockMvc로 HTTP 계약·권한·검증 | `IntegrationTest` |
| 아키텍처 | 계층·컨텍스트 의존 규칙 | ArchUnit |

순서: 도메인 규칙 테스트부터 시작해 바깥(application → API)으로 넓힌다.

## 5. 커밋 (아주 잘게)
TDD 사이클이 그대로 히스토리에 보이게 한다.
```
test(resume): 파싱 전 결과 수정은 거부한다          ← red: 테스트 실패, 컴파일은 됨
feat(resume): 상태 전이 불변식 구현                  ← green
refactor(resume): 전이 검사를 requireStatus로 추출   ← refactor (선택)
```
- **red 커밋도 컴파일은 돼야 한다.** 새 클래스나 메서드가 필요하면 `UnsupportedOperationException`을 던지는 뼈대를 함께 넣는다.
- **PR의 마지막 커밋(head)은 항상 green이다.** CI는 head만 검사한다.
- 리팩터(구조 이동)는 테스트가 green인 상태를 유지하며, 이동 단위별로 커밋한다.
- 커밋 하나에는 논리 변경 하나만 담는다. "그리고"로 설명해야 하면 나눈다.
