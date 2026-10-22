# Review 001: 2026-10-22-schedule-generation

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22T21:01+09:00
- Consumed handoff: HANDOFF-001.md
- Reviewed source base / HEAD: `019ea93363af7ca7a398c4868ee33bc519a43d75`, branch `feat/spring/schedule-generation`; uncommitted `git diff HEAD` snapshot.
- Snapshot: all **37/37** entries in `HANDOFF-001-SHA256.txt` matched before source review, including `HANDOFF-001.patch` (`175333ba149fd0cc3f5d2b50b0a638a448ec6503da5b216ca9e53bb377151071`). Unrelated untracked `.vscode/` and `spring-api/.claude/` excluded.
- Verdict: **CHANGES_REQUESTED**
- Scope and criteria checked: AC-01..AC-07; splitting and validation, callers/persistence, locks/transactions, JST ranges, monthly isolation, cron, binding/error mapping, SC-014, scripts, docs 02/04/05/06/07/08/09/11/12, TODO.md and AGENTS.md. Assumed the documented MVP workload, including concurrent manual and scheduled generation.

## Findings

### R-01 [P2] A stored break outside business hours creates bookable slots outside business hours

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/domain/hospital/model/HospitalBusinessHours.java:63-67`.
- Trigger / reproduction: Load a pre-validation row with opening hours `09:00-18:00`, break `19:00-20:00`, and 30-minute slots, then call `slotsOn(2026-11-02)`. Also test break `07:00-08:00`. The old API and V5 table allow these rows; new validation does not repair them.
- Expected and observed behavior: Clipping must keep every slot within opening hours (or reject/skip the invalid row). The first case produces AVAILABLE slots `18:00-18:30` and `18:30-19:00`; the second produces `08:00-08:30` and `08:30-09:00`. Only one side of each break boundary is clamped, so the AVAILABLE segment extends outside opening hours.
- User or system impact: Both generation paths persist slots patients can book while the hospital is closed. Later runs skip the populated day, so fixing the hours alone does not repair these slots.
- Evidence and confidence: High. Reviewer executed the compiled domain class through a temporary Java source-mode probe; both cases reproduced. Generator saves these slots unchanged. Existing tests cover zero duration and reversed hours, but not disjoint breaks.
- Suggested correction and regression check: Clamp both endpoints to the full opening interval, or explicitly skip invalid stored hours. Test before-opening, after-closing and partially overlapping breaks; assert every slot stays within opening hours.
- Disposition: open; required fix.

### R-02 [P2] Accepted second-precision times are silently rounded down

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/domain/hospital/model/HospitalBusinessHours.java:80-81`, used by `slotsOn` at lines 54-64.
- Trigger / reproduction: `HospitalBusinessHours.create(..., 09:00:30, 10:00:30, null, null, 30)` succeeds, then `slotsOn` generates `09:00-09:30` and `09:30-10:00`. The DTO uses `LocalTime` with no minute-precision constraint; persistence uses SQL `time`. API input and existing rows can contain nonzero seconds.
- Expected and observed behavior: Preserve accepted opening/break boundaries or explicitly reject unsupported precision. Integer division by 60 moves the first slot before opening and similarly moves post-break availability before a second-precision break ends.
- User or system impact: Generated availability disagrees with stored hours and docs/06 section 18, even for valid new input.
- Evidence and confidence: High. The probe called actual `create` and `slotsOn` methods and printed the output above. Normal UI input is minute-based, but the backend does not enforce the frontend date helper's assumption that seconds are always zero.
- Suggested correction and regression check: Preserve precision using non-wrapping arithmetic, or define/validate minute-only input and handle existing second-precision rows safely. Test opening and break endpoints with nonzero seconds. Synchronize API/error documentation if precision is restricted.
- Disposition: open; required fix.

### R-03 [P3] Zero-result messages infer facts the response does not establish

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:264-266`.
- Trigger / reproduction: Register valid Monday hours `09:00-09:15`, no break, duration 30 minutes, then generate a month. Hours exist but segments are too short: `{createdSlots:0, generatedDates:0, skippedDates:0}`. With one manual Monday slot, the response instead has `skippedDates:1`, while other Mondays still have no slots.
- Expected and observed behavior: The first response tells the administrator to register hours that already exist. The second says every business day already has slots, which is false. Counts describe existing-slot skips, not why every other date produced nothing.
- User or system impact: Incorrect explanation hides the duration/segment mismatch.
- Evidence and confidence: High by source tracing of validation, splitting, counts and UI conditional; browser reproduction not executed.
- Suggested correction and regression check: Report zero created slots and the actual skipped count neutrally; suggest checking hours/duration without asserting missing hours or complete coverage. Check these fixtures and a current month with no remaining configured weekdays.
- Disposition: open; minor correction, not the basis for blocking acceptance.

## Verification

Reviewer commands used this snapshot on 2026-10-22, Windows PowerShell, JDK 17.0.19. No application source, tests, product docs, handoff or task metadata were changed. Temporary probe removed; `spring-api/.gradle/review-home` retained.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: status/branch/HEAD; `Get-FileHash -Algorithm SHA256` for every manifest path | 37/37 matched; branch and HEAD matched | None relied on | Uncommitted snapshot |
| Spring initial attempt | `spring-api/`: `./gradlew.bat clean test --offline` | Exit 1: default `C:\.gradle` wrapper lock directory inaccessible | None | Retried below |
| AC-01..06 / Spring | `spring-api/`: set `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home`; `./gradlew.bat clean test --offline` | Exit 0; BUILD SUCCESSFUL; XML totals **316 tests, 0 failures, 0 errors, 0 skipped** | Handoff also reports 316 | H2/mocks do not prove PostgreSQL concurrency |
| R-01/R-02 | Root: `java --class-path <compiled main classes;cached spring-web jar> spring-api/build/ReviewProbe.java` | Exit 0; actual domain outputs reproduced findings | None | Domain probe, not HTTP/DB; initial JShell attempts failed and are not evidence |
| AC-07 build | `frontend/`: `npm.cmd run build` | Exit 0; TypeScript and Vite 6.4.3 passed, 136 modules | Browser observations in HANDOFF-001 | Browser not rerun |
| PostgreSQL | Root: `docker info --format '{{.ServerVersion}}'` | Docker config/engine pipe access denied; acceptance script not run | Handoff: GEN-01..09 9/9; regression 12/12 | SQL/locks/races **reported-only**; scripts inspected |
| Docs | Compared requested doc diffs and relevant design sections with code | Endpoint/errors/roles/counts/range/transactions/cron/test/status entries synchronized; no separate blocking doc finding | None | Update docs for any changed precision or invalid-data policy |

Additional source and passing-test checks:

- Generator locks the non-deleted hospital before reading hours/existing dates. Writes occur in a separate proxied transactional bean. No transaction surrounds the monthly loop; per-hospital exceptions are caught, allowing later hospitals to proceed. Existing dates, closed weekdays and result counts agree with the API specification.
- Manual service checks affiliation first and uses one JST date for today and this-month..+12 bounds. Monthly selection excludes deleted hospitals and filters ACTIVE from the initial list. Scheduler uses `0 0 0 1 * *`, `Asia/Tokyo`, next month; scheduling is enabled. No real cron trigger or concurrent suspension run was executed.
- Business-hours DTO/controller validation protects required fields. Valid minute-based frontend requests, including omitted breaks, remain compatible; invalid inputs deliberately return HOSPITAL-009. HOSPITAL-009/010 map to 400. Missing/unparsable YearMonth cases passed WebMvc tests. Method authorization and Long principal handling are present; full HTTP authorization evidence is reported-only.
- UI default/min/max use JST. The refresh guard and store request sequencing prevent a late generation response from replacing the selected hospital's schedules. R-03 remains an incorrect explanation of valid zero-result cases.

## Remaining risks and next action

Claude Code should fix R-01/R-02, address or explicitly dispose of R-03, add focused regressions, and publish HANDOFF-002 with fresh hashes. Rerun affected tests/build and synchronize changed API rules before re-review. Existing green tests do not cover the reproduced boundary defects.

The handoff's existing limitations are distinct: individual manual slot creation can overlap generated slots; current/next-month bootstrap is manual; whole-day skipping intentionally does not fill partial days. PostgreSQL and browser evidence remains implementer-reported; cron firing itself is unverified. This review does not perform integration.
