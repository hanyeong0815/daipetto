# Handoff 002: 2026-10-26-security-portability

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: see TASK.md "Updated at"
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: review only (round 2)
- Consumed review: REVIEW-001.md (not edited)
- Source base SHA: `3241f26`; head: uncommitted working tree on `chore/security-and-portability`
- Dirty snapshot manifest: `HANDOFF-002-SHA256.txt` (same file rule as HANDOFF-001)

No application code, test code or dependency changed since HANDOFF-001; only `scripts/osv-scan.mjs`, `.ai-collab/scripts/*`, `.gitignore` and documentation.

## Response to REVIEW-001

| ID | Disposition | Change | Evidence |
|---|---|---|---|
| R-01 | Fixed | `run()` quotes the executable and arguments only on Windows (where the `.bat` needs the shell) and passes them raw with `shell: false` elsewhere; the Gradle wrapper is passed as a plain path. The repository root now comes from the script location (`import.meta.url`), so the scan no longer depends on the working directory | Windows run from `frontend/`: `scanned 406 packages (Maven 80, npm 300, PyPI 26); vulnerable 3` (same as before). Non-Windows branch not executable here (reported limitation) |
| R-02 | Fixed | Python packages are read from an installed environment with `importlib.metadata` (no uv needed): `--python <interpreter>`, else `analysis-api/.venv`, else `.review-venv`. If none exists the script prints an actionable error and exits 2 instead of producing an incomplete report; the requirements-file fallback was removed | 26 PyPI packages (transitive included) from `.venv`; `--python /nonexistent/python` → exit 2 with the message, before the Gradle/npm collection and before any network call |
| R-03 | Fixed | `run-codex.sh` keeps each attempt's output separately, judges the rate limit from that attempt only, and exits with the companion's status; `CODEX_COMPANION` and `CODEX_RETRY_SECONDS` overrides added. New self-check `.ai-collab/scripts/test-run-codex.sh` with fake companions | `test-run-codex.sh`: success 0, non-retry failure 7, rate limit then ok 0, rate limit exhausted 2, ok after a limit already in the log 0, companion missing 1 — all OK |
| R-04 | Fixed | `.claude/settings.local.json` added to `.gitignore` | `git check-ignore -v .claude/settings.local.json` → `.gitignore:56`; `.claude/settings.json` not ignored; `git ls-files -ci --exclude-standard` → 0 |
| R-05 | Fixed | docs/10 both frontend tables (Vite 6.4.4, Axios 1.20.0, React Router DOM 6.30.6, Capacitor 8.4.3), both `frontend-dev` agent definitions (Vite 6.4.4, Capacitor 8.4.3), README (Axios 1.20) | `grep` for the old versions outside the frozen `docs/archive/` and docs/09 §22's intentional before/after table → none |
| R-06 | Fixed | README test block: each project command runs in a subshell from the repository root; the scan stays at the root (and now works from any folder anyway) | file review |

## Acceptance evidence

HANDOFF-001's test evidence still applies to the unchanged application snapshot (JUnit 382, pytest 38, API regression 134/134, E2E 17/17, browser S-01..S-16, image builds). New evidence for this round is in the table above.

## Receiver instructions

Verify `HANDOFF-002-SHA256.txt`. Re-check R-01..R-06, run `.ai-collab/scripts/test-run-codex.sh` (no network needed) and `node scripts/osv-scan.mjs --python /nonexistent/python` (must exit 2 without network). JUnit/pytest reruns are optional since no code changed. Write `REVIEW-002.md`; keep R-01..R-06 IDs, new findings from R-07.
