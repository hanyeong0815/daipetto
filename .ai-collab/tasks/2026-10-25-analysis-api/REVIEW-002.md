# Review 002: 2026-10-25-analysis-api

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-25T03:41+09:00
- Consumed handoff: HANDOFF-002.md
- Reviewed source base / head: `3ee6e6b70267dd54c2600b842b98e2b24b5f8a3e`, dirty working tree on `feat/analysis/analysis-api`.
- Snapshot: all **65 entries** in `HANDOFF-002-SHA256.txt` matched SHA-256 hashes calculated from the repository root before source review and after verification.
- Verdict: **ACCEPTED**
- Scope and criteria checked: R-01..R-03, the added Python test, TASK.md AC-01..AC-06 regression impact. Focused on paths whose hashes changed since HANDOFF-001, with the relevant domain callers, Python schemas/services and docs/07 §7. Unrelated working-tree files were excluded.

## Findings

### R-01 [P2] The relay accepts non-2xx responses with a success-shaped body

- Disposition: **resolved**.
- Evidence: `AnalysisApiClient.java:59` installs an `onStatus` predicate rejecting every status outside 200..299 before deserialization, throwing ANALYSIS-003. `AnalysisApiConfig.java:25` overrides `prepareConnection`, calls the superclass, then disables instance redirects, overriding its GET default.
- Regression checks: `redirect_status_becomes_unavailable` rejects a 302 with a valid-looking success body. `AnalysisApiConfigTest.does_not_follow_redirect` uses the production factory against a real loopback JDK HTTP server: 302 becomes ANALYSIS-003 and only the original path is requested, although the target would return 200. The companion test reads 200 and verifies the internal key header. Existing 401/500 and I/O tests also pass.

### R-02 [P2] Missing required analysis fields pass the upstream response check

- Disposition: **resolved**.
- Evidence: compact constructors in `domain/analysis/model/WeightAnalysis.java`, `HealthScore.java` and `VaccinationForecast.java` enforce required fields. `List.copyOf` rejects null lists and null elements and retains valid empty lists. Nested point, summary, period, factor and vaccination-item constructors check their required fields with `requireNonNull`. Required numeric/boolean components use boxed types, so omitted values cannot silently become zero/false. Jackson construction failures propagate through `RestClientException` to ANALYSIS-003.
- Regression checks: all 12 `incomplete_body_becomes_unavailable` cases pass, covering absent/null collections, a null element and missing required nested fields, including numeric and boolean fields. Existing success cases pass for empty weight points, null weight summary, NO_DATA score, null factor value, null lastCompletedDate and additional unknown fields.
- Compatibility review: compared every constructor component with docs/07 §7 and the Python schemas. Nullable `summary`, `changeRate`, `score`, factor `value`, `lastCompletedDate` and `vaccinationId` remain nullable. Empty vaccination items/factors remain allowed; their constructors are also exercised by the service tests. The controller's vaccination fixture now supplies `7L` for boxed `Long daysUntil`; all callers compile. No new contract mismatch found.

### R-03 [P3] Current documentation still contains pre-FastAPI and skeleton descriptions

- Disposition: **resolved**.
- Evidence: docs/07 §7-3 now references routers/schemas/services/repository and the Spring relay. AGENTS.md §1 describes the three implemented analysis endpoints. docs/10 §6-3 lists all implemented modules and test files. Both analysis-api-dev definitions describe the implemented FastAPI structure and preserve the symptoms practice gap. TODO and docs/07 §7-5 agree with the status and required-field handling fixes.
- Consistency check: no stale Django View/Serializer or skeleton-only instruction found in the handoff-listed current documentation and agent definitions. Remaining Django references explain the framework migration. AGENTS.md §10's skeleton mention describes the completed initial phase and is followed by the implemented phase-2 routes; it does not instruct readers to treat the current project as skeleton-only.

No new actionable finding; R-04 was not allocated.

## Verification

Commands were executed by the reviewer against the snapshot above. No application source or implementer-owned document was edited.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Repository root: parse each manifest entry and calculate `Get-FileHash -Algorithm SHA256` | 65/65 matched before review and after verification; branch/HEAD match handoff | None | Dirty snapshot acceptance, not a new commit |
| Python suite | `analysis-api/`: `.review-venv/Scripts/python.exe -m pytest -q` | Exit 0; **38 passed**. One nonfatal PytestCacheWarning: cache path creation denied | Handoff count agrees | Fake DB connections do not prove real SQL |
| Fresh Spring suite | `spring-api/`: `gradlew.bat test --offline --rerun-tasks`, absolute `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home` | Exit 0; **6 tasks executed**; fresh JUnit XML loaded with XML-aware encoding: **382 tests, 0 failures, 0 errors, 0 skipped**; JaCoCo generated | Handoff count agrees | Existing mapper/JVM warnings were nonfatal; H2/mocks do not prove PostgreSQL behavior |
| Analysis regression suites | Fresh JUnit XML and source inspection | Service 9, controller 9, client 20, config 2: **40 total**, all passed. Increase from REVIEW-001's 367 to 382 matches 13 added client cases and 2 config tests | None | Real HTTP exercised only on loopback |
| Added Python test | `test_next_vaccinations_same_day_schedule_uses_smaller_id_and_latest_completion`, included in the full pytest run | Passed. Unsorted same-date schedules choose id 7 over 9; several completions choose 2025-05-03; completed-only forecast is 2026-05-03, RECOMMENDED. Assertions agree with unchanged service logic and docs/07 §7-4 | None | No PostgreSQL query execution |
| Rest of change | Compare round-1/round-2 manifest hashes; inspect affected callers, constructor serialization contract, Python schemas/services and current documentation | Python production source, Spring owner/period checks, controller routes, configuration properties and remaining application paths retain round-1 hashes. Full suites found no regression. Practice gap remains intact | Prior Docker/build/read-only checks; refreshed E2E 17/17 | Docker, PostgreSQL and E2E were **not rerun** |

## Remaining risks and next action

- Docker image/runtime, PostgreSQL read-only enforcement, real SQL and the refreshed 17-item E2E result remain implementer-reported. This acceptance does not independently verify them.
- Symptoms/summary remains the intentional practice gap; frontend charts and Notion synchronization remain outside this review. The agreed heuristic score and per-request DB connections are unchanged limitations.
- `.review-python`, `.review-venv` and `spring-api/.gradle/review-home` were retained. Only this reviewer-owned review record was added, apart from normal test/build outputs.
- Next actor: Claude Code/coordinator may consume this review and update task status for this same snapshot. Commit, push, merge, deployment and Notion synchronization were not performed by the reviewer.
