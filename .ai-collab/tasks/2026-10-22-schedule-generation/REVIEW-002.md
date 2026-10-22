# Review 002: 2026-10-22-schedule-generation

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22T21:10+09:00
- Consumed handoff: [HANDOFF-002.md](HANDOFF-002.md), response to [REVIEW-001.md](REVIEW-001.md)
- Reviewed source base / HEAD: `019ea93363af7ca7a398c4868ee33bc519a43d75`, branch `feat/spring/schedule-generation`; uncommitted `git diff HEAD` snapshot.
- Snapshot: **37/37** entries of `HANDOFF-002-SHA256.txt` verified from the repository root before source review, including `HANDOFF-002.patch` (`b30970ad0d9b207c4d97b81cc490449a1b44304cba641a6f0a85ad966b485977`). Unrelated untracked `.vscode/` and `spring-api/.claude/` excluded.
- Verdict: **ACCEPTED**
- Scope and criteria checked: R-01/R-02 domain fixes and regression boundaries (AC-01/AC-06), R-03 zero-result copy (AC-07), generator consumption/counts, related documentation, whole Spring suite and frontend build. Assumed the documented MVP workload. This is a focused re-review, not a new PostgreSQL concurrency or browser acceptance run.

## Findings

No new actionable regression found in the inspected scope. All three existing findings are resolved.

### R-01 [P2] Stored breaks outside business hours

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/domain/hospital/model/HospitalBusinessHours.java:59`.
- Disposition: **resolved**.
- Evidence: Both endpoints are clamped to the complete opening interval. Breaks require both stored endpoints; an empty or reversed clamped interval falls back to a single AVAILABLE segment. Partial overlaps retain only their in-hours BLOCKED segment. The generator consumes these slots without changing their boundaries.
- Regression checks: The new `slots_stay_within_opening_hours_for_invalid_stored_breaks` test passed. Reviewer also executed actual compiled domain methods for breaks entirely before/after hours, overlapping either end, covering the whole opening interval, start-only, end-only, equal endpoints and reversed endpoints. All nine cases matched exact expected slots/statuses and stayed within opening hours.

### R-02 [P2] Second-precision boundaries rounded down

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/domain/hospital/model/HospitalBusinessHours.java:55`, `:74`.
- Disposition: **resolved**.
- Evidence: `toNanoOfDay` / `ofNanoOfDay` and long arithmetic preserve opening and break boundaries. Whole slots are included only when their end is at or before the segment end. For validated durations (5..240 minutes), arithmetic cannot overflow and an endpoint cannot wrap past midnight. No minute-only input restriction was introduced.
- Regression checks: `slots_keep_second_precision` and existing remainder/no-wrap tests passed. Reviewer independently checked valid `create` input `09:00:30-10:00:30`, nanosecond opening/break boundaries, a segment one nanosecond too short, an exact endpoint at `23:59:59.999999999`, and a late segment that would require crossing midnight. All five cases matched exact expected results.

### R-03 [P3] Zero-result message inferred an unsupported cause

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:263`.
- Disposition: **resolved**.
- Evidence: Every zero-created response now reports that no slots were created, shows the actual `skippedDates`, and suggests checking hours and duration. It no longer asserts missing hours or that every business day already has slots. Source tracing covers short segments with zero skips, short segments plus one manually populated day, and no remaining configured weekdays. Positive-result copy and schedule refresh behavior remain compatible.
- Regression check: TypeScript/Vite build passed. Browser rendering was **not** independently exercised; the browser observation in HANDOFF-002 is reported-only.

## Verification

Reviewer execution: Windows PowerShell, JDK 17.0.19, Node 24.17.0, the snapshot above, 2026-10-22. Only this review record was authored; normal build outputs were regenerated. `spring-api/.gradle/review-home` was retained.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: branch/HEAD/status; `Get-FileHash -Algorithm SHA256` for every manifest entry | 37/37 matched; branch/HEAD matched | None relied on | Dirty snapshot, not a commit |
| Spring / regression | `spring-api/`: set `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home`; `./gradlew.bat clean test --offline` | Exit 0, BUILD SUCCESSFUL; fresh XML totals **318 tests, 0 failures, 0 errors, 0 skipped** | Handoff also reports 318 | H2/mocks do not prove PostgreSQL behavior |
| R-01/R-02 independent probe | Root: stdin-fed `jshell --class-path <spring-api/build/classes/java/main;cached spring-web-6.1.21.jar> --feedback concise -` | Exit 0; **14/14** named exact-output assertions printed PASS; actual compiled domain classes used | PostgreSQL legacy-row examples in HANDOFF-002 | Domain-only; JShell emitted registry preferences access warnings |
| AC-07 build | `frontend/`: `npm.cmd run build` | Exit 0; TypeScript and Vite 6.4.3 passed, 136 modules | Handoff browser zero-result observation | Browser not rerun |
| PostgreSQL | Root: `docker info --format '{{.ServerVersion}}'` | Exit 1: Docker config and engine pipe access denied; acceptance script not run | HANDOFF-002: GEN-01..09 9/9 and legacy-row R-01/R-02 HTTP/SQL examples | PostgreSQL, SQL locks/races and HTTP acceptance remain **reported-only** |
| Docs | Reviewed affected diffs, especially docs/06 section 18, docs/11 GEN-T001, docs/12 section 3-18 and AGENTS section 8 | Clamping/one-sided-break policy, precision, coverage and 318-test count agree with implementation; no new API restriction needs documenting | None relied on | TODO/AGENTS review status remains coordinator-owned |

The initial XML counting command used PowerShell's default text encoding and failed to parse Japanese test names. Repeating with explicit UTF-8 parsed all result files successfully; only the corrected totals above are evidence. No test failure resulted from that reporting error.

## Remaining risks and next action

PostgreSQL and browser evidence remains implementer-reported. Real cron firing was not verified. Existing limitations remain: frontend time labels show HH:mm even for second-precision slots; individual manual slot creation can overlap generated slots; whole-day skipping does not fill partially populated days; initial current/next-month generation is manual. These are not regressions from the reviewed fixes.

Claude Code, as coordinator, can consume this review and update task/status records before the separately authorized integration workflow. No source fix is requested. Acceptance applies to this frozen snapshot and does not mean merge, push or deployment occurred.