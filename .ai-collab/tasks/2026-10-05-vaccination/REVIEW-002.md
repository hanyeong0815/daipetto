# Review 002: 2026-10-05-vaccination

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-21T23:10+09:00 (reviewer system clock)
- Consumed handoff: HANDOFF-002.md, response to REVIEW-001.md.
- Reviewed source: `feat/spring/vaccination`, base/HEAD `6e18153cd7010c73a5771285b045cb3bdc3e55aa`, plus the frozen dirty snapshot in HANDOFF-002-SHA256.txt. All 56 entries, including the patch and four script files, matched SHA-256 from the root before code review and after verification.
- Verdict: ACCEPTED
- Scope and criteria checked: R-01..R-04 against their original triggers; related AC-05/06/09 behavior and AC-01..08 regression suite. Inspected VaccinationPage/HealthRecordPage keyed children, failed-load/retry branches, PetDetailPage, routes and pet store context, reminder service/port/adapter/query/entity/domain, reschedule, notification persistence, scheduler, V12 backfill, regression tests and supplied scripts. Compared docs/05/06/07/08/11/12, TODO.md and AGENTS.md with implementation.

## Findings

No new actionable defect was found in the reviewed changes.

### R-01 [P2] Previous pet's actionable vaccination rows after a failed switch

- Disposition: resolved.
- Evidence: `frontend/src/pages/VaccinationPage.tsx:29` renders `VaccinationList key={petId}`. A route key change replaces the component owning rows, form and date action. Effect cleanup rejects old responses; `loadFailed` hides rows and the empty message after a failed load. `HealthRecordPage.tsx:19` similarly isolates records and editing state by pet.
- Regression check: Executed unchanged TSX through the installed TypeScript transpiler and an inline hook/router/API harness. For both pages, loaded A with record 101, checked different A/B child keys, disposed A and mounted B, rejected B, and asserted that A's record is absent and error/retry is present. Retrying with an empty successful response clears the error and shows the correct empty message. This is an in-memory component check, not React DOM reconciliation or a browser run. Late responses cannot populate a different keyed instance; VaccinationList also has an active-effect guard.

### R-02 [P2] Pet detail reports failed retrieval as no scheduled vaccinations

- Disposition: resolved.
- Evidence: `frontend/src/pages/PetDetailPage.tsx:16` keeps `upcomingFailed` separately. A request clears failure and enters loading; rejection sets failure without manufacturing an empty result. Rendering prioritizes the failure message and retry over loading/empty/populated states.
- Regression check: The inline harness rejected the vaccination request while supplying a loaded pet. The panel contained the failure message and no no-plans message. Invoking retry and resolving an empty list produced the no-plans message. The populated-success branch remains intact; the handoff's populated retry browser result is reported-only.

### R-03 [P2] An old reminder suppresses the rescheduled vaccination's reminder

- Disposition: resolved.
- Evidence: `SendVaccinationRemindersService.execute` claims the extracted scheduled date before notification insertion in the same transaction. `VaccinationJpaRepository.markReminded` requires SCHEDULED, an undeleted row, the extracted date, and a NULL marker. `reschedule` resets the marker in its conditional UPDATE. Notification insertion failure rolls back the claim with the service transaction.
- Original trigger: If D1 is extracted and cancel/reschedule to D2 commits before the claim, the date predicate rejects D1 and leaves D2 unclaimed. If the claim obtains the row first, reschedule waits and then clears the marker, leaving the new schedule eligible. An unchanged repeat claim affects zero rows. Re-registering the same date can use the existing target because its notification still describes the current date; it does not suppress a different date's reminder.
- Regression check: Independently reran the service skip-on-failed-claim and H2 adapter tests in the full suite. They cover D1 extraction followed by cancel/reschedule to D2, rejection of D1, D2 eligibility/claim, repeat-claim rejection, and re-extraction after an earlier reminder. Inspected the PostgreSQL script that observes a claim blocked behind an uncommitted date change, then checks zero old notifications, one D2 notification and no third-run notification. Its execution is reported-only.
- Migration: V12 adds nullable DATE `reminded_date`; its EXISTS backfill preserves the old `created_at >= updated_at` classification. Rows without a qualifying old notification remain NULL. Entity/domain mapping and insert/update paths agree; no full-row update was introduced. V12 preserves legacy classification rather than repairing any historical data already misclassified by the old race. The reported three-case V11-to-V12 backfill run was not independently reproduced.

### R-04 [P3] Screen copy promises daily reminders

- Disposition: resolved.
- Evidence: `VaccinationPage.tsx:128` now promises one eligible reminder at 09:00 and another for a changed schedule. docs/05 SC-016 agrees. docs/06 records the marker/V12; docs/07/08 describe claiming/clearing it; docs/11 maps regression coverage; docs/12 explains the former timestamp race. TODO.md and AGENTS.md use the new rule and reserve V13 onward for later migrations.
- Regression check: Compared copy with the daily eligibility check and passing repeat-claim/dedupe tests. Daily repeated notification is no longer promised.

## Verification

Reviewer commands used the snapshot above, Windows, JDK 17.0.19, Gradle 8.14.4 and Node 24.17.0.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: parse all manifest entries and compare `Get-FileHash -Algorithm SHA256`; branch/HEAD/status | 56/56 matched twice; branch/base match | None | Unrelated untracked .vscode/ and spring-api/.claude/ excluded |
| AC-01..08 | spring-api/: set GRADLE_USER_HOME to existing spring-api/.gradle/review-home; `./gradlew.bat --offline --no-daemon clean test` | Exit 0, BUILD SUCCESSFUL; 49 XML suites, 256 tests, zero failures/errors/skips | Handoff also reports 256 passing | Mocks/H2 do not prove PostgreSQL locking or Flyway |
| Result aggregation | Root: load TEST-*.xml with System.Xml.XmlDocument.Load | Exit 0; counts above verified | None | Initial PowerShell default-encoding XML read failed; encoding-aware XML loading resolved it without rerunning tests |
| AC-09 build | frontend/: `npm.cmd run build` | Exit 0; TypeScript/Vite passed, 135 modules | Handoff browser checks | No native runtime verification |
| R-01/R-02 and HealthRecordPage | frontend/: inline Node harness, TypeScript transpiler, unchanged source, mocked hooks/router/API | Exit 0; both keyed-list switch/failure/retry checks and detail failure/empty/retry passed | Handoff manual browser switch and populated retry | Not browser/React DOM integration; HealthRecord create/edit/delete browser flows unrun |
| R-03 real concurrency, AC-06 migration/backfill | Root: `docker version`; inspect scripts/README.md and all three supplied scripts | Exit 1: configuration access and engine named-pipe permission denied; no database/server started | PostgreSQL 16.9 V1..V12 + validation, blocked-claim test, V11 backfill cases, 13/13 HTTP checks | Reported-only; not independently rerun |
| Architecture/docs | Source tracing, changed docs, TODO/AGENTS; branch inventory and pet-fields migration tree | Marker ownership, conditional writes, transaction boundary and docs agree for the fixes; pet-fields retains documented V4 collision | None | Historical status/test entries are not fresh evidence; original TASK AC-05 timestamp rule is superseded by this handoff |

MapStruct again warned about vaccination complete/reschedule and hospital suspend methods. Mapping tests passed; no mapping defect was demonstrated.

## Remaining risks and next action

- PostgreSQL and browser execution remain reported-only as detailed above. Actual cron firing and Android/iOS runtime behavior were not tested. The batch-wide transaction and documented single-scheduler deployment assumption remain unchanged.
- Acceptance is limited to this frozen snapshot and re-review. It does not certify unrelated pre-existing frontend state flows or imply integration/deployment.
- Claude Code, as coordinator, can record acceptance in the next task/status update and proceed with authorized next work. No further fix is requested for R-01..R-04.
- REVIEW-002.md is the only authored file. Application source, product docs, TASK, handoffs, REVIEW-001 and scripts were not edited. Required test/build commands generated normal outputs. spring-api/.gradle/review-home was reused and retained; no cache deletion, commit, push or merge occurred.
