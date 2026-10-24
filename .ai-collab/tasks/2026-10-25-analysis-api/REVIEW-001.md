# Review 001: 2026-10-25-analysis-api

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-25T03:30+09:00
- Consumed handoff: HANDOFF-001.md
- Reviewed source base / head: `3ee6e6b70267dd54c2600b842b98e2b24b5f8a3e`, dirty working tree on `feat/analysis/analysis-api`.
- Snapshot: all **62 entries** in `HANDOFF-001-SHA256.txt` matched SHA-256 hashes calculated from the repository root before code review; all 62 still matched after verification.
- Verdict: **CHANGES_REQUESTED**
- Scope and criteria checked: TASK.md AC-01..AC-06; analysis-api source, requirements, image configuration and tests; Spring analysis domain/use cases/client/controller, configuration and tests; docs/07 §7, docs/09 §4-6, AGENTS.md §3; handoff-listed documentation changes, README/TODO and framework rename.

## Findings

### R-01 [P2] The relay accepts non-2xx responses with a success-shaped body

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/client/analysis/AnalysisApiClient.java:56`; request factory configuration in `AnalysisApiConfig.java:24`.
- Trigger / reproduction: using the compiled client in JShell with the existing Spring test dependencies, bind `MockRestServiceServer` to a RestClient builder and return `withStatus(HttpStatus.FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"success\":true,\"data\":{\"points\":[],\"summary\":null}}")` for `getWeight(7L, null, null)`.
- Expected and observed behavior: docs/07 §7-5 requires every non-2xx response to become ANALYSIS-003. Observed `302 RESULT=WeightAnalysis[points=[], summary=null]`, with no exception. The default retrieve error handling does not reject 3xx; the client never checks the actual status. The production request factory also leaves automatic GET redirects enabled, which can hide the original non-2xx status.
- User or system impact: an upstream redirect or routing error can be returned to the caller as a successful analysis instead of 503.
- Evidence and confidence: reviewer-executed JShell reproduction against the hashed implementation; high confidence. Existing client tests cover 401/500, not 3xx.
- Suggested correction and regression check: reject statuses outside 200..299 explicitly and disable automatic redirects if the documented contract is to apply to the original upstream response. Add a 302 response with a valid-looking envelope and a redirect-to-200 case; both must produce ANALYSIS-003. Keep valid 200 behavior passing.
- Disposition: open; required fix.

### R-02 [P2] Missing required analysis fields pass the upstream response check

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/client/analysis/AnalysisApiClient.java:61` and the records in `domain/analysis/model/`.
- Trigger / reproduction: in the same JShell harness, return HTTP 200 JSON `{"success":true,"data":{}}`, then call `getWeight(7L, null, null)`.
- Expected and observed behavior: this is not the WeightData response specified in docs/07 §7-1: `points` must be an array, including for no data. Observed `EMPTY DATA RESULT=WeightAnalysis[points=null, summary=null]`. Jackson supplies null/default values for missing record fields; checking only that `data` exists does not establish a readable analysis response. The same omission can leave health-score grade/period/factors or vaccination items null.
- User or system impact: an incompatible or misrouted upstream service response is exposed as HTTP 200 with an invalid data contract, rather than ANALYSIS-003. Consumers cannot distinguish it from successful analysis.
- Evidence and confidence: reviewer-executed reproduction against the real client; high confidence. The malformed-body test only uses invalid JSON and `success:false,data:null`.
- Suggested correction and regression check: validate required response fields at the client boundary, mapping invalid shapes to ANALYSIS-003. Test missing/null `points`, `items`, and required health-score fields. Preserve legitimate empty arrays, weight `summary:null`, NO_DATA `score:null`, nullable factor values, and tolerance for additional fields.
- Disposition: open; required fix.

### R-03 [P3] Current documentation still contains pre-FastAPI and skeleton descriptions

- File and line: `docs/07_API_Design 363d097bd4f78052a200e297abde89f3.md:1030`; `AGENTS.md:23`; `docs/10_Development_Environment 366d097bd4f78035ba4aef5e47b56378.md:330`.
- Trigger / reproduction: follow the symptoms practice exercise's reference implementation guidance or read the current area/directory summaries.
- Expected and observed behavior: docs/07 §7-3 still calls the implementation `View → Serializer → Service`, although 7-1 uses Router/Pydantic schemas and no Django view or serializer. AGENTS §1 still says `skeleton only` despite three implemented analysis routes. The docs/10 directory tree lists only the initial five app modules, omitting routers/schemas/services/repository.
- User or system impact: the practice exercise and project status describe the wrong implementation structure. This weakens the requested documentation/code consistency.
- Evidence and confidence: direct comparison with the hashed source and TODO's correct Router/Schema/Service/Repository guidance; high confidence. References explaining the historical Django-to-FastAPI decision are intentional and are not defects.
- Suggested correction and regression check: update these three current descriptions to the implemented FastAPI structure; search the handoff-listed docs for stale framework terms and distinguish historical rationale from current instructions.
- Disposition: open; nonblocking documentation cleanup, suitable for the correction round.

## Verification

All commands below target the same dirty snapshot above. No application source or implementation-owned document was edited.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Repository root: `Get-FileHash -Algorithm SHA256` for every manifest path, before review and after tests | 62/62 matched both times | None | None |
| Spring suite | `spring-api/`: `gradlew.bat test --offline`, with absolute `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home` | Exit 0, but tasks UP-TO-DATE | Handoff: 367 passed | Cached run alone is not fresh execution |
| Fresh Spring suite | Same directory/environment: `gradlew.bat test --offline --rerun-tasks` | Exit 0, all 6 tasks executed. Fresh JUnit XML: **367 tests, 0 failures, 0 errors, 0 skipped**. Analysis suites: service 9, controller 9, client 7. JaCoCo report generated | Handoff count agrees | H2/mocks do not prove PostgreSQL behavior; existing mapper and Windows preference warnings were nonfatal |
| Python suite | `analysis-api/`: `.venv/Scripts/python.exe -m pytest -q` | Exit 1 before pytest: cannot launch the original interpreter under the user's uv-managed Python installation | Handoff: 37 passed | No reviewer pytest pass claim |
| Python fallback | `analysis-api/`: `uv venv --python 3.12.10 .venv-review`; retried with cache/install locations inside `.venv-review`, then `uv python install 3.12.10` | Default uv cache and installed Python access denied. Isolated download failed after retries with socket permission error 10013. No usable interpreter was created, so `uv pip install --python .venv-review -r requirements-dev.txt` could not run | None | Temporary `.venv-review` and its isolated cache/install directories were removed; original `.venv` retained |
| R-01/R-02 | Repository root: JShell, compiled main classes plus existing Spring/Jackson/Micrometer jars from review-home; MockRestServiceServer against actual AnalysisApiClient | Exit 0; 302 accepted; empty data object accepted with null points, as shown above | None | Mock HTTP response, not a live upstream |
| AC-01/02 security | Read main/router/security/db/errors and common/route tests | Router key dependency precedes DB dependency; unset key denies; UTF-8 bytes passed to constant-time comparison; no docs/redoc/OpenAPI routes or CORS; no exception details in error responses | Python tests and container smoke in handoff | Runtime Python checks unavailable. Existing no-DB tests track SQL/repository calls, not entry into the DB dependency itself |
| AC-03 SQL/session | Read db.py/repository.py and compose/image settings | Read-only connection startup option, autocommit and connection timeout configured. Queries use only static SQL fragments and bound parameters; soft-deleted health/vaccination rows excluded | Scratch PostgreSQL column/read-only smoke and image build | **Container, PostgreSQL and E2E results are reported-only; not rerun** |
| AC-04/06 calculations | Compare services.py and service tests against docs/07 §7-1/7-2/7-4 | No calculation mismatch found: greatest weighted id per day, ascending dates, Decimal half-up rounding including negatives, rounded penalty thresholds, today-inclusive 30 days, symptom-day dedupe/cap/whitespace, NO_DATA, scheduled precedence and id tie-break, latest completed date + calendar year, leap day, trimmed-name grouping, date ordering, overdue and signed daysUntil; today uses Asia/Tokyo | pytest 37; scratch-DB E2E 17/17 | Python execution unavailable. Scheduled same-date id tie-break and latest of multiple completed rows are implemented but not explicitly covered by the supplied service tests |
| AC-05 architecture/security | Trace Controller → UseCase → Service → AnalysisClient port → adapter; inspect PetPersistenceAdapter/PetJpaRepository and SecurityConfig | Long principal; authenticated boundary; owner checks precede client calls; repository excludes deleted pets; missing/other-owner pets rejected; reversed period rejected; bound date queries; no writes/JPA dependencies added to analysis models; URL/key properties agree with env and compose; configured timeouts 2s/5s | E2E ownership/auth/upstream-down results | Runtime timeout durations not exercised by mock tests; upstream handling defects are R-01/R-02 |
| Practice gap/docs | Inspect routes/use cases, tests and handoff-listed documentation changes | symptoms/summary absent on both sides; both suites include absence checks; TODO records the exercise and frontend exclusion. Main framework/routing/config documentation is consistent apart from R-03 | Notion sync explicitly pending | Frontend/native work excluded; no Notion sync performed |

## Remaining risks and next action

- Claude Code should fix R-01/R-02 with focused client regression tests, clean up R-03, and publish a new numbered handoff with refreshed snapshot hashes. Preserve the symptoms practice gap.
- Rerun pytest in an environment with usable Python 3.12.10 before acceptance; the 37-test result remains implementer-reported. Docker/PostgreSQL read-only enforcement, real SQL and the 17-item E2E result also remain reported-only.
- Per-request DB connections without a pool are a documented MVP choice, not a finding. The health score is the agreed reference heuristic; clinical validity is outside this review.
- `spring-api/.gradle/review-home` was retained as requested. This review does not commit, push, merge, deploy, or update coordinator-owned task status.
