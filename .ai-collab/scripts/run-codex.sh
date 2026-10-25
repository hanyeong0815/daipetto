#!/usr/bin/env bash
# Runs a Codex task from a prompt file through the Claude Code `codex` plugin and retries
# while Codex reports its usage limit (.ai-collab/SETUP.md §4).
# Usage: .ai-collab/scripts/run-codex.sh <prompt-file> <log-file> [max-wait-minutes]
# Env:   CODEX_MODEL          model for the task (default gpt-6.1-sol; set it to "" for the Codex default)
#        CODEX_COMPANION      path to codex-companion.mjs (default: newest installed plugin version)
#        CODEX_RETRY_SECONDS  wait between rate-limited attempts (default 1200)
# Exit:  the companion's exit status of the last attempt (0 = finished; read the log),
#        1 = plugin not found, 2 = still rate-limited after max-wait
set -u
PROMPT_FILE="$1"; LOG="$2"; MAX_WAIT_MIN="${3:-110}"
MODEL="${CODEX_MODEL-gpt-6.1-sol}"
RETRY_SECONDS="${CODEX_RETRY_SECONDS:-1200}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPANION="${CODEX_COMPANION:-$(printf '%s\n' "$HOME"/.claude/plugins/cache/openai-codex/codex/*/scripts/codex-companion.mjs | sort -V | tail -1)}"
if [ ! -f "$COMPANION" ]; then
  echo "codex plugin (openai-codex marketplace) is not installed for Claude Code" >&2
  exit 1
fi
cd "$ROOT" || exit 1
attempt_out="$(mktemp)"
trap 'rm -f "$attempt_out"' EXIT
start=$(date +%s)
attempt=0
while :; do
  attempt=$((attempt + 1))
  echo "=== attempt $attempt at $(date '+%F %T') model=${MODEL:-default}" >> "$LOG"
  node "$COMPANION" task --write ${MODEL:+--model "$MODEL"} "$(cat "$PROMPT_FILE")" > "$attempt_out" 2>&1
  status=$?
  cat "$attempt_out" >> "$LOG"
  # judge the rate limit from this attempt only, not from earlier attempts in the log
  if tail -40 "$attempt_out" | grep -qi "usage limit\|rate limit"; then
    elapsed_min=$(( ($(date +%s) - start) / 60 ))
    if [ "$elapsed_min" -ge "$MAX_WAIT_MIN" ]; then
      echo "=== still rate-limited after ${elapsed_min} min; giving up for now" >> "$LOG"
      exit 2
    fi
    echo "=== rate-limited; waiting ${RETRY_SECONDS}s" >> "$LOG"
    sleep "$RETRY_SECONDS"
    continue
  fi
  echo "=== exit status $status" >> "$LOG"
  exit "$status"
done
