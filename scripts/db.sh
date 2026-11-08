#!/usr/bin/env bash
# 로컬 Postgres(pgvector) 관리. Windows에서는 WSL의 docker를, 그 외에는 로컬 docker를 사용한다.
# 사용: bash scripts/db.sh up | down | status | psql [db] | reset
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if command -v docker >/dev/null 2>&1; then
  dc() { (cd "$ROOT" && docker compose "$@"); }
  dk() { docker "$@"; }
else
  # Windows(Git Bash): WSL 안의 docker 사용. MSYS 경로 자동변환을 끈다.
  WIN_ROOT="$(cygpath -m "$ROOT" 2>/dev/null || echo "$ROOT")"
  dc() { MSYS_NO_PATHCONV=1 wsl --cd "$WIN_ROOT" docker compose "$@"; }
  dk() { MSYS_NO_PATHCONV=1 wsl docker "$@"; }
fi

is_ready() {
  dk exec classmento-postgres pg_isready -U classmento -d classmento >/dev/null 2>&1
}

case "${1:-status}" in
  up)
    if is_ready; then echo "DB 실행 중 (localhost:5433)"; exit 0; fi
    dc up -d
    for _ in $(seq 1 40); do
      if is_ready; then echo "DB 준비 완료 (localhost:5433)"; exit 0; fi
      sleep 1
    done
    echo "DB가 40초 안에 준비되지 않았습니다"; exit 1 ;;
  down)   dc down ;;
  status) if is_ready; then echo "running"; else echo "stopped"; exit 1; fi ;;
  psql)   dk exec -it classmento-postgres psql -U classmento -d "${2:-classmento}" ;;
  reset)
    # 데이터 전부 삭제 후 재생성
    dc down -v && "$0" up ;;
  *) echo "사용: $0 up|down|status|psql [db]|reset"; exit 2 ;;
esac
