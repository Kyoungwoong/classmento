#!/usr/bin/env bash
# Claude Code PostToolUse 훅: 편집된 파일을 자동 포맷한다. 실패해도 작업을 막지 않는다(exit 0).
input="$(cat)"
file="$(printf '%s' "$input" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write((j.tool_input&&j.tool_input.file_path)||"")}catch{}})')"
[ -z "$file" ] && exit 0

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
norm="$(printf '%s' "$file" | tr '\\' '/')"

case "$norm" in
  */frontend/src/*.ts|*/frontend/src/*.tsx|*/frontend/src/*.css)
    (cd "$ROOT/frontend" && pnpm -s exec prettier --write "$file" >/dev/null 2>&1) ;;
  */backend/src/*.java)
    (cd "$ROOT/backend" && ./gradlew -q spotlessApply >/dev/null 2>&1) ;;
esac
exit 0
