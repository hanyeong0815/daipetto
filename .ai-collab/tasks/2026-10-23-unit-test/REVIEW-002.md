# Review 002: 2026-10-23-unit-test

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-23T05:20+09:00
- Consumed handoff: HANDOFF-002.md
- Reviewed source: branch `test/unit-test`, base/HEAD `7ee0db60e1d42acbf7e11f361a632ba56791791c`, plus frozen dirty snapshot `HANDOFF-002-SHA256.txt`.
- Snapshot verification: **309/309 entries matched**, computed from the repository root before test-content inspection; **309/309 matched again after verification**.
- Verdict: **CHANGES_REQUESTED**
- Scope and criteria checked: final round, AC-01..05; all REVIEW-001 findings and weak checks, API/JUnit/spec consistency, screen evidence links, evidence secret scan, D-001..009 dispositions, fresh JUnit and frontend build. PostgreSQL and browser execution are reported-only.

The final evidence is internally consistent, and the independently rerun JUnit suite passes all 342 tests. The remaining required correction is the backend configuration step in the reproduction instructions (R-02). No new product defect was found in the inspected changes.

## Findings

### R-02 [P2] Root-directory backend command does not load the Spring DB configuration

- File and line: `test/README.md:56-69`, especially line 68; `spring-api/src/main/resources/application-local.yml:3-7`.
- Trigger / reproduction: follow the README from the repository root, with Docker running but without separately exported Spring datasource credentials. The build command returns to the root before `java -jar ... --spring.profiles.active=local --DB_NAME=daipetto_ut`.
- Expected and observed behavior: the documented sequence should start the isolated backend. The local profile imports `optional:file:.env[.properties]` relative to the working directory and requires `DB_PASSWORD`, with `DB_USERNAME` defaulting to `your_db_user`. The root `.env` has `POSTGRES_USER`/`POSTGRES_PASSWORD`, not these Spring keys; neither the shell's `DB_USERNAME`/`DB_PASSWORD` nor its `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` is set. The separate `spring-api/.env` is not loaded by this command. The README supplies neither that configuration step nor an explicit alternate config location.
- User or system impact: a recipient following the stated prerequisites cannot reproduce the API run; AC-05 and the original R-02 reproduction requirement remain incomplete.
- Evidence and confidence: static configuration/working-directory trace, README inspection, and key-name/presence-only checks (no credential values recorded). High confidence. Backend startup was not executed; this is not a claimed PostgreSQL rerun.
- Suggested correction and regression check: document preparing the Spring `.env` from `spring-api/.env.example` with credentials matching the scratch PostgreSQL instance, then launch the jar from `spring-api/` (return to the root in the runner terminal), or explicitly import that config location from the root. Retain the scratch DB and port overrides. Verify the exact documented sequence from a fresh terminal and scratch DB, then publish a new handoff/hash manifest. No product-code change is needed.
- Disposition: **open**, narrowed from REVIEW-001. README creation, evidence locations, and the real review link are resolved; backend configuration is still missing.

### Previous-finding dispositions

| Stable ID | Disposition | Reviewed evidence and assessment |
|---|---|---|
| G-01 | Resolved | `HospitalScheduleGenerationSchedulerTest` invokes the entry method, captures exactly one next-JST-month argument, rejects extra interactions, and checks monthly cron/zone. Production uses `YearMonth.now(ServerTime.ZONE_ID).plusMonths(1)`. A controlled rollover is not simulated; accepted as a disclosed test limitation for this direct calculation. |
| G-02 | Resolved | `VaccinationReminderSchedulerTest` invokes `remind()`, checks exactly one today's-JST argument and daily 09:00 JST cron/zone. Controlled UTC/JST boundary simulation remains a disclosed limitation, not an untested entry method. |
| G-03 | Resolved | UT-03-012 sends PATCH weights -0.01 and 1000, checks 400/VALIDATION-001 and unchanged weight. The update DTO has both nonnegative and digit constraints. |
| G-04 | Resolved | UT-04-014 exercises reschedule's missing-ID response; `reschedule_vaccination_fail_when_transition_lost` reads a valid CANCELLED vaccination, forces repository update false, asserts VACCINATION-003 and no save. |
| G-05 | Resolved | UT-09-013 and UT-10-009 exercise missing cancel/complete; complete additionally checks the aggregate snapshot, including notification count, is unchanged. |
| G-06 | Resolved | UT-12-007 calls activation for self and missing target, asserting USER-005/400 and USER-003/404. |
| G-07 | Resolved | UT-13-008 contains all 15 previously listed route/method pairs with ROLE_USER, plus four system-admin-only routes with ROLE_HOSPITAL_ADMIN: 19 recorded denials, 403/AUTH-005. Nine aggregate row/state measures are compared before/after. This is an aggregate side-effect check, not an exhaustive row-content comparison. |
| G-08 | Resolved | UT-07-009 rejects unmanaged business-hours PATCH/DELETE and missing-hospital PATCH; UT-08-010 rejects unmanaged unblock and verifies BLOCKED remains. The three required operation guards are exercised. |
| G-09 | Resolved | UT-07-010 sends reversed hours, 241-minute slots and out-of-hours breaks through PATCH, asserting HOSPITAL-009 and unchanged stored hours. |
| G-10 | Resolved | New RefreshToken/HealthRecord/Vaccination/CreateReservation/GetReservation service tests cover every listed missing relation branch with the expected codes. Mutation tests prevent the relevant writes; list and detail both cover absent hospital/schedule/pet. |
| G-11 | Resolved | `HospitalControllerTest.unexpected_exception_returns_server_error` checks 500, false success, absent/null data, SERVER-001, exact Japanese message and no internal secret text. |
| R-01 | Resolved | `lib.mjs` masks JWT-shaped strings in recorded SQL, DB results, response strings and check values while executing the original SQL. Password object fields remain masked. Final evidence scan found no complete JWT or fixture-password bytes. This does not assert that arbitrary future runner exception/raw-body strings are universally sanitized. |
| Weak-check note | Resolved | UT-13-004/005/007 check COMMON-001/002/003 and the error envelope. UT-10-004 checks total notification count, recipient and type; UT-10-005 compares the exact ordered pair of types with recipients, catching duplicates or a wrong recipient. |

### Product and documentation defect dispositions

| IDs | Assessment and coverage |
|---|---|
| D-001 | Sound documentation correction to VALIDATION-001. UT-01-003/004 exercise registration validation; current common handler implements that contract. |
| D-002 | Sound documentation correction: authenticated Long principal, no request body needed, user-row lock and bulk refresh revocation, `data.isLogout=true`. UT-01-013/016 and existing logout unit tests cover the contract. Historical cause/date is not independently re-audited. |
| D-003..D-005 | Specific Spring exceptions now map to 404/COMMON-001, 405/COMMON-002 and 415/COMMON-003 instead of the generic 500 handler. Controller regressions and UT-13-004/005/007 cover the fixes. |
| D-006 | Docs enumerate the actual six `PetType` constants, including BIRD. UT-02-005 uses unsupported DRAGON and checks VALIDATION-001. No enum support was removed. |
| D-007 | The separate area field flows through `fetchHospitals(keyword, area)` to the API's `area` parameter; the existing latest-response guard remains in use. UT-14-016 records before/after area-search evidence. Browser retest is reported-only. |
| D-008 | Generic failure message is Japanese and excludes exception details; covered by the fresh SERVER-001 JUnit regression above. |
| D-009 | Registration catch uses the existing `extractErrorMessage` helper; JSX renders the API message with a fallback. UT-14-023/033 record validation and duplicate-email messages and DB checks. Browser retests are reported-only. |

## Verification

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Frozen target | Repository root: parse manifest `hash *path` entries and compare `Get-FileHash -Algorithm SHA256 -LiteralPath`; inspect branch, HEAD and dirty/untracked status | 309/309 initially and finally; branch/HEAD match handoff | None | Unrelated tool-installation/report files excluded as handed off |
| AC-01/02 | Inspect new API definitions, changed/new JUnit tests, connected implementation/configuration and doc diffs; compare all 134 API definitions with JSON metadata | All previous coverage findings addressed; defect dispositions above | Real HTTP/DB execution | Scheduler boundary simulation and cron-trigger execution not performed |
| AC-03 API/spec | Read-only Node comparisons of definitions, `_summary.json`, every API JSON and all UT-01..13 section-1 rows | 134 definitions = 134 unique results = 134 evidence files; all OK, nonempty checks all true, no recorded errors; per-feature totals and metadata/expectations/verdicts agree | 134/134 PostgreSQL run | Response/DB authenticity not independently replayed |
| AC-03 JUnit/spec | Parse stored and fresh XML testcase identities/counts; compare every section-2 row and RESULTS feature counts | Stored/fresh/spec identities match exactly: **342 tests, 0 failures/errors/skips** | None for fresh JUnit | H2/mocks do not prove PostgreSQL SQL/constraints/concurrency |
| AC-03 fresh JUnit | `E:/workspace/daipetto/spring-api`: PowerShell `$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle/review-home'; ./gradlew.bat test --offline --rerun-tasks` | **Exit 0; BUILD SUCCESSFUL in 1m 10s; all 6 tasks executed**, including JaCoCo | None | Nonfatal MapStruct and Windows Java preferences warnings |
| Frontend build | `E:/workspace/daipetto/frontend`: `npm.cmd run build` | **Exit 0**, TypeScript and Vite build succeed | None | No browser/native execution |
| AC-04 links | Resolve every Markdown local link in UT-01..14; count screen rows | **562 local links exist**, including all UT-14 evidence targets; **33 screen rows** | 33/33 browser results, confirm/select/toast adaptations disclosed in spec | Screenshot existence does not independently prove interaction outcomes |
| AC-05 secrets | Read-only recursive evidence byte/text scan for complete three-part JWT shape and the fixture password read from `lib.mjs`; inspect masking paths | **0 matching evidence files** | None | Byte scan is not OCR of screenshots |
| AC-05 reproduction | Inspect README commands, local Spring config, example config and relevant environment-key presence | README/review links exist; root startup configuration mismatch confirmed | Scratch DB execution and cleanup | **R-02 open** |

Per-feature API/JUnit counts (UT-01..13): **16/41, 13/28, 12/27, 14/54, 11/27, 8/8, 10/29, 10/29, 13/32, 9/30, 3/12, 7/20, 8/5**. These agree with spec headers and RESULTS.md; totals are 134/342. Reclassification of common-error tests into UT-13 explains the lower UT-02/05 counts relative to round 1.

Environment: Windows PowerShell, Microsoft OpenJDK 17.0.19, Gradle wrapper 8.14.4, Node 24.17.0, Vite 6.4.3. The reviewer-owned Gradle daemon was stopped using the same review home after testing to close its retained output pipe; the test command then returned exit 0. **`spring-api/.gradle/review-home` is preserved.** Published evidence was not regenerated or overwritten. Only this review record was authored; test/build caches and outputs were generated by the requested commands.

## Remaining risks and next action

Claude Code should correct R-02's environment setup/working-directory instructions, validate those exact commands on a fresh scratch PostgreSQL database, and publish HANDOFF-003 with updated hashes. Preserve the consumed records and evidence; no further product changes are requested by this review.

PostgreSQL API/DB evidence and all browser results remain **implementer-reported only**. Docker/PostgreSQL and browser runs were not attempted by this reviewer. No claim is made for PostgreSQL concurrency, live cron triggering, or Capacitor runtime behavior. Acceptance is withheld for the remaining AC-05 reproduction defect, not for the disclosed execution limits.
