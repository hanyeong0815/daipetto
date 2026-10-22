# Review 003: 2026-10-23-unit-test

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-23T05:29+09:00
- Consumed handoff: HANDOFF-003.md
- Reviewed source: branch `test/unit-test`, base/HEAD `7ee0db60e1d42acbf7e11f361a632ba56791791c`, plus frozen dirty snapshot `HANDOFF-003-SHA256.txt`.
- Snapshot verification: **309/309 entries matched** from the repository root before test-content inspection; **309/309 matched again after review**.
- Verdict: **ACCEPTED**
- Scope and criteria checked: narrow round-3 re-verification of the only open finding, R-02 (AC-05), regenerated API evidence/spec/result totals (AC-03), and the requested secret scan (AC-05). Earlier resolved findings retain their REVIEW-002 dispositions.

## Findings

### R-02 [P2] Backend reproduction configuration — resolved

- File and line: `test/README.md:60-78`; `spring-api/src/main/resources/application-local.yml:3-7`; `spring-api/.env.example` (key names only); `test/scripts/lib.mjs:10-11`; `test/DEFECTS.md:35` (T-006).
- Original trigger: launching the local-profile backend from the repository root did not import `spring-api/.env`, leaving required Spring datasource configuration unavailable.
- Expected and observed behavior: README section 5 now prepares `spring-api/.env` from the example, maps `DB_USERNAME`/`DB_PASSWORD` to the PostgreSQL credential keys, and launches the jar from `spring-api/` in a separate terminal. The local profile imports `optional:file:.env[.properties]`, so the documented working directory now selects the intended configuration. The scratch overrides `--DB_NAME=daipetto_ut` and `--server.port=18081` remain explicit; the runner stays at the repository root and waits for the startup message.
- Evidence and confidence: static command/configuration trace confirms the original configuration defect is corrected. Example key names include `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`; no `.env` values were read or printed. The runner passes `process.env.PGUSER ?? 'daipetto_user'` as the `psql -U` argument through `execFileSync`; README documents overriding it when the PostgreSQL username differs. T-006 records the same cause, correction, and reported fresh-shell/scratch-DB rerun. High confidence in the static correction; runtime replay remains reported-only.
- Regression check: reviewer-executed configuration/working-directory and PGUSER argument inspection. HANDOFF-003 reports successful backend startup, 134/134 API items, and scratch DB cleanup using the revised sequence.
- Disposition: **resolved**. No new actionable finding in the selected scope.

## Verification

All reviewer commands ran from `E:/workspace/daipetto` at the source revision above, using PowerShell and Node 24.17.0. Read-only scripts used `fs`, `crypto`, and `assert/strict`; no evidence was regenerated.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Frozen target | Root: parse every `HANDOFF-003-SHA256.txt` entry and compare SHA-256 of file bytes; inspect Git branch/HEAD/status | 309/309 matched initially and finally; branch/HEAD agree with handoff | None | Dirty snapshot acceptance only; no integration |
| Build rerun scope | Root: compare path/hash maps of HANDOFF-002 and HANDOFF-003 for the union of `spring-api/` and `frontend/` entries | All 13 entries unchanged, including Java tests, scheduler tests, product changes and `build.gradle` | REVIEW-002 already records independent JUnit/build success | JUnit and frontend build deliberately not rerun, as requested |
| AC-05 / R-02 | Root: inspect README section 5, local-profile import/placeholders, example key names, PGUSER argument and T-006 | Configuration load sequence and DB username override agree | HANDOFF-003 fresh-shell/scratch-DB run: backend started, 134/134 OK, cleanup completed | PostgreSQL not rerun by reviewer; Docker availability not probed |
| AC-03 API consistency | Root: Node assertions compare summary feature totals/results, all UT-01..13 API rows and headers, RESULTS feature/total rows, and individual evidence IDs/verdicts | 134 unique summary results = 134 spec rows = 134 API evidence files; 134 OK, 0 NG; all feature counts agree | HTTP/DB execution at `2026-10-23T05:26:27+09:00` | Internal consistency does not independently replay responses or DB checks |
| AC-05 secrets | Root: recursively scan all file bytes in `test/evidence`, `test/spec`, `test/reviews`, and root `test/*.md`; compare fixture-password bytes read internally from `lib.mjs` and full three-part JWT pattern | 771 files scanned; 0 full JWT matches, 0 fixture-password matches; no secret values printed | None | Byte/text scan is not screenshot OCR |
| Review ownership | Create only this REVIEW-003.md; check review cache presence | `spring-api/.gradle/review-home` remains present | None | No application, evidence or earlier record edits |

API counts for UT-01..13: **16, 13, 12, 14, 11, 8, 10, 10, 13, 9, 3, 7, 8** (total **134**).

The first consistency-script attempt exited 1 because PowerShell encoded Japanese literals in piped JavaScript incorrectly. The corrected ASCII/Unicode-escape assertions exited 0 and completed the comparisons and secret scan. This was a reviewer-script issue, not a package failure.

## Remaining risks and next action

The PostgreSQL rerun remains **implementer-reported only**, as do browser results from earlier rounds. No fresh JUnit, frontend build, PostgreSQL, browser, concurrency, cron-trigger, or Capacitor execution is claimed in this round. REVIEW-002's disclosed limitations remain applicable.

R-02 is closed and the selected frozen snapshot is accepted. The coordinator may record this acceptance in TASK.md at the next handoff boundary. This verdict does not merge or deploy the work.
