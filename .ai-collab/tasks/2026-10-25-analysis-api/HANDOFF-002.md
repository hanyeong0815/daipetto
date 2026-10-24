# Handoff 002: 2026-10-25-analysis-api

- Author / implementer: Claude Code (fixes: main session Opus 5.5; full test runs and E2E: Sonnet 5.5 subagent)
- Publication: ready
- Timestamp: 2026-10-25T03:38+09:00
- Task specification: TASK.md
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: review only
- Consumed review: REVIEW-001.md (not edited)
- Source base SHA: `3ee6e6b`
- Source head SHA: none — uncommitted working tree on `feat/analysis/analysis-api`
- Diff basis: `git diff 3ee6e6b` plus the untracked paths in the manifest
- Dirty snapshot manifest: `HANDOFF-002-SHA256.txt` (same file rule as HANDOFF-001)

## Response to REVIEW-001

| ID | Disposition | Change | Regression evidence |
|---|---|---|---|
| R-01 | Fixed | `AnalysisApiClient`: `.onStatus(status -> !status.is2xxSuccessful(), ...)` throws ANALYSIS-003 for every non-2xx including 3xx. `AnalysisApiConfig`: the request factory overrides `prepareConnection` and calls `setInstanceFollowRedirects(false)`, so a redirect is reported as the original 302 | `AnalysisApiClientTest.redirect_status_becomes_unavailable` (MockRestServiceServer 302 with a success-shaped body); new `AnalysisApiConfigTest` with a real JDK `HttpServer`: 302 → ANALYSIS-003 and the redirect target is never requested; 200 is read and the key header is sent |
| R-02 | Fixed | Required fields are checked in the compact constructors of the domain records (`List.copyOf` for `points`/`factors`/`items`, which also rejects null elements; `requireNonNull` for every field docs/07 does not mark nullable). Primitive fields became boxed so a missing field is not silently 0/false. Jackson's construction failure surfaces as `RestClientException` → ANALYSIS-003 | Parameterized `incomplete_body_becomes_unavailable` (12 cases: empty data, null/null-element points, missing weight, missing summary count, missing grade/period/factors, missing factor penalty, missing items, missing nextDate, null overdue). Existing valid cases still pass (empty arrays, `summary: null`, `score: null`, null factor value, unknown extra field) |
| R-03 | Fixed | docs/07 §7-3 reference now names `routers.py → schemas.py → services.py → repository.py` and the Spring relay classes; AGENTS §1 no longer says skeleton; docs/10 §6-3 tree lists routers/schemas/services/repository and the test files; both `analysis-api-dev` agent definitions updated. Remaining Django mentions are the intentional decision history (docs/04 §5-2, docs/10 §6-1, docs/13 §3-4, TODO decision line, AGENTS §10) | `grep` for `View → Serializer`, `Serializer`, `skeleton`, `土台` in current docs: no current-instruction hits |
| Coverage note (scheduled same-date tie-break, latest of several completions) | Added | `test_next_vaccinations_same_day_schedule_uses_smaller_id_and_latest_completion` | pytest |
| Python not runnable in the reviewer sandbox | Mitigated | A workspace-local interpreter for review: `analysis-api/.review-python/` (uv-managed CPython 3.12.10) and `analysis-api/.review-venv/` (its `pyvenv.cfg` home points inside the workspace; requirements-dev installed). Both are excluded locally via `.git/info/exclude` and are not part of the snapshot | `analysis-api/.review-venv/Scripts/python.exe -m pytest -q` → 38 passed (implementer) |

Docs also updated for the fixes: docs/07 §7-5 (redirects not followed; 3xx and missing required fields → ANALYSIS-003), TODO.md, AGENTS.md §8/§10 counts.

## Acceptance evidence (2026-10-25)

| Check | Command | Outcome |
|---|---|---|
| Spring | `spring-api/`: `./gradlew clean test --offline` (Sonnet subagent) | 382 tests, 0 failures/errors/skips (analysis: service 9, controller 9, client 20, config 2) |
| Python | `analysis-api/`: `.venv/Scripts/python.exe -m pytest -q` (Sonnet subagent); `.review-venv` (implementer) | 38 passed |
| E2E | scratch DB `daipetto_an_e2e2`, analysis container 18084 (current source mounted), Spring jar 18083 rebuilt after the fixes; `scripts/analysis-e2e.mjs` (Sonnet subagent) | 17/17 OK — real analysis-api responses pass the stricter Spring validation; `evidence/e2e-result.json` overwritten, no JWT/fixture password; DB dropped |

## Limitations

Unchanged from HANDOFF-001 (Docker/PostgreSQL/E2E implementer-reported; heuristic score; no pool; `test/` package excludes the analysis APIs; Notion sync after acceptance).

## Receiver instructions

Verify `HANDOFF-002-SHA256.txt`. Run pytest with `analysis-api/.review-venv/Scripts/python.exe -m pytest -q` from `analysis-api/` (do not modify or delete `.review-python`/`.review-venv`), and JUnit with `GRADLE_USER_HOME=spring-api/.gradle/review-home --offline` (compare with 382; use `--rerun-tasks` if UP-TO-DATE). Check R-01..R-03 and that valid responses still pass. Write `REVIEW-002.md`; do not edit REVIEW-001 or HANDOFF-001.
