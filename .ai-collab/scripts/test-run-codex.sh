#!/usr/bin/env bash
# Self-check for run-codex.sh with fake companions (no network, no Codex needed).
# Usage: .ai-collab/scripts/test-run-codex.sh   (exit 0 = all cases pass)
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"; R="$DIR/run-codex.sh"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
printf 'prompt' > "$T/p.txt"
printf 'console.log("review done")\n' > "$T/ok.cjs"
printf 'console.error("auth failed"); process.exit(7)\n' > "$T/fail.cjs"
printf 'console.log("usage limit reached"); process.exit(1)\n' > "$T/limit.cjs"
printf 'const fs=require("fs"),f=process.env.STATE,n=fs.existsSync(f)?+fs.readFileSync(f,"utf8"):0;fs.writeFileSync(f,String(n+1));if(n===0){console.log("You have hit your usage limit");process.exit(1)}console.log("done")\n' > "$T/once.cjs"
fails=0
check() { local name="$1" want="$2"; shift 2; "$@" >/dev/null 2>&1; local got=$?; if [ "$got" = "$want" ]; then echo "OK   $name ($got)"; else echo "FAIL $name: want $want, got $got"; fails=$((fails + 1)); fi; }
check "success"                 0 env CODEX_COMPANION="$T/ok.cjs" "$R" "$T/p.txt" "$T/a.log"
check "non-retry failure"       7 env CODEX_COMPANION="$T/fail.cjs" "$R" "$T/p.txt" "$T/b.log"
check "rate limit, then ok"     0 env STATE="$T/s" CODEX_RETRY_SECONDS=1 CODEX_COMPANION="$T/once.cjs" "$R" "$T/p.txt" "$T/c.log"
check "rate limit exhausted"    2 env CODEX_RETRY_SECONDS=1 CODEX_COMPANION="$T/limit.cjs" "$R" "$T/p.txt" "$T/d.log" 0
check "ok after limit in log"   0 env CODEX_COMPANION="$T/ok.cjs" "$R" "$T/p.txt" "$T/d.log"
check "companion missing"       1 env CODEX_COMPANION="$T/none.cjs" "$R" "$T/p.txt" "$T/e.log"
exit "$fails"
