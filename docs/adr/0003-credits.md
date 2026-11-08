# ADR 0003: 크레딧 결제 (설계 메모 — 아직 미구현)

- 상태: 제안 (2026-11-08)

## 방향
- **원장(ledger) 방식:** 잔액 컬럼 하나를 덮어쓰지 않는다. 모든 증감을 행으로 쌓고, 잔액은 합계(또는 캐시 컬럼과 대조)로 구한다. 환불·분쟁·감사 대응이 쉽다.
  ```
  credit_ledger(id, user_id, delta, reason, ref_type, ref_id, created_at)
    reason: PURCHASE | INTERVIEW_SESSION | RESUME_PARSE | REFUND | ADMIN_ADJUST
  payment(id, user_id, provider, provider_payment_id, amount_krw, credits, status, created_at)
  ```
- **차감 시점:** AI 비용이 드는 행위(면접 세션 시작, 평가) **직전**에 차감한다. 실패하면 같은 `ref_id`로 환불 행을 추가한다.
- **동시성:** 차감은 `SELECT ... FOR UPDATE`(사용자 행 잠금) 또는 잔액 캐시 컬럼의 조건부 UPDATE로 처리해 마이너스를 막는다.
- **PG사:** 토스페이먼츠 등 국내 PG. 결제 승인은 서버에서 검증한다(웹훅 + 금액 재확인).
- **관리자:** 사용자별 잔액·원장 조회, 수동 조정(ADMIN_ADJUST, 사유 필수).

## 지금 해둔 것
- 모든 도메인 테이블에 `user_id`가 있다. 비용이 드는 작업은 서비스 계층 한 곳(`*Service`)에서 시작한다. 이 두 가지 덕분에 차감 지점을 끼워 넣기 쉽다.
