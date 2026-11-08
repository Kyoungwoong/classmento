#!/usr/bin/env bash
# 디자인 시스템 기계 검사 (docs/design/design-system.md 금지 목록 중 코드로 잡을 수 있는 것)
# verify.sh와 GitHub Actions CI가 함께 사용한다.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/frontend/src"
status=0

# 1) 임의 색(#hex)·임의 px 클래스 금지 → 토큰 사용
hits=$(grep -rnE '\[#[0-9a-fA-F]{3,8}\]|-\[[0-9.]+px\]' "$SRC" --include='*.tsx' --include='*.ts' || true)
if [ -n "$hits" ]; then
  echo "토큰 대신 임의값을 쓴 곳이 있습니다:"
  echo "$hits"
  status=1
fi

# 2) 그라데이션·블러(glassmorphism) 금지
hits=$(grep -rnE 'bg-gradient|bg-linear|bg-radial|linear-gradient|radial-gradient|backdrop-blur' "$SRC" || true)
if [ -n "$hits" ]; then
  echo "금지된 그라데이션/블러:"
  echo "$hits"
  status=1
fi

[ $status -eq 0 ] && echo "디자인 검사 통과"
exit $status
