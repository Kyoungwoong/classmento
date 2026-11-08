#!/usr/bin/env bash
# 전체 검증. 사용: bash scripts/verify.sh [--fe | --be]
# 작업을 "완료"라고 말하기 전에 반드시 통과해야 한다.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-all}"
FAILED=()

step() {
  local name="$1"; shift
  echo ""
  echo "▶ $name"
  if "$@"; then
    echo "✓ $name"
  else
    echo "✗ $name"
    FAILED+=("$name")
  fi
}

design_tokens_only() {
  # 디자인 시스템: 임의 색(#hex)·임의 px 클래스 금지 (docs/design/design-system.md 금지 목록)
  local hits
  hits=$(grep -rnE '\[#[0-9a-fA-F]{3,8}\]|-\[[0-9.]+px\]' "$ROOT/frontend/src" --include='*.tsx' --include='*.ts' || true)
  if [ -n "$hits" ]; then
    echo "토큰 대신 임의값을 쓴 곳이 있습니다:"
    echo "$hits"
    return 1
  fi
  # 그라데이션 금지
  hits=$(grep -rnE 'bg-gradient|bg-linear|bg-radial|linear-gradient|backdrop-blur' "$ROOT/frontend/src" || true)
  if [ -n "$hits" ]; then
    echo "금지된 그라데이션/블러:"
    echo "$hits"
    return 1
  fi
}

if [ "$MODE" = "all" ] || [ "$MODE" = "--fe" ]; then
  cd "$ROOT/frontend" || exit 1
  step "FE format"     pnpm -s format:check
  step "FE lint"       pnpm -s lint
  step "FE typecheck"  pnpm -s typecheck
  step "FE test"       pnpm -s test
  step "FE design"     design_tokens_only
  step "FE build"      pnpm -s build
fi

if [ "$MODE" = "all" ] || [ "$MODE" = "--be" ]; then
  # 백엔드 테스트는 실제 Postgres(classmento_test)를 쓴다
  step "DB up"         bash "$ROOT/scripts/db.sh" up
  cd "$ROOT/backend" || exit 1
  step "BE spotless"   ./gradlew -q spotlessCheck
  step "BE test"       ./gradlew -q test
fi

echo ""
if [ ${#FAILED[@]} -eq 0 ]; then
  echo "✅ verify 통과"
else
  echo "❌ 실패: ${FAILED[*]}"
  exit 1
fi
