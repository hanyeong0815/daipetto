# Review 001: 2026-10-05-vaccination

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-21T22:58+09:00
- Consumed handoff: HANDOFF-001.md, revised 2026-10-21; first review of this snapshot.
- Reviewed source: `feat/spring/vaccination`, HEAD/base `6e18153cd7010c73a5771285b045cb3bdc3e55aa`, plus the frozen dirty snapshot. All 52 HANDOFF-001-SHA256.txt entries (48 changed files, three scripts, patch) passed `sha256sum -c` from the root before review and again before publication.
- Verdict: CHANGES_REQUESTED
- Scope and criteria checked: AC-01 through AC-09. Cancel/delete are normal scope under TASK.md's 2026-10-21 scope change. Inspected controller, DTO, services, domain, ports, adapters, queries, migration, scheduler, notification creation, tests, frontend routes/API/types/pages/date helpers, docs/02,05,06,07,08,11, TODO.md and AGENTS.md.

## Findings

### R-01 [P2] A failed pet switch exposes the previous pet's actionable vaccination rows

- File and line: `frontend/src/pages/VaccinationPage.tsx:41-46` (state at line 29).
- Trigger / reproduction: Load pet A's vaccinations, navigate within the same route component to pet B, then fail B's list request. The effect sets loading but does not clear or associate `items` with a pet. Its catch only sets an error; finally clears loading.
- Expected and observed behavior: B must never render A's records. Instead A's records reappear under B's URL with complete/cancel/delete controls. The `active` flag rejects late responses but does not invalidate already loaded data.
- User or system impact: An owner of both pets can change or delete A's vaccination while viewing B. Backend ownership checks do not prevent this mistake.
- Evidence and confidence: High. Reviewer transpiled the unchanged TSX in memory and executed it with a minimal hook/router/API harness. After resolving A with vaccination 101, switching the parameter to B, and rejecting B, the rendered tree contained A's vaccine and action controls, the fetch error, and `isLoading=false`. This is a component-state reproduction, not a browser test.
- Suggested correction and regression check: Associate loaded state with petId and render rows only for the current pet, or reset pet-specific state on a route change. Invalidate the open date action too. Test A-success/B-failure and A-late-response/B-success; no A controls may remain on B.
- Disposition: open.

### R-02 [P2] Pet detail reports failed retrieval as no scheduled vaccinations

- File and line: `frontend/src/pages/PetDetailPage.tsx:27`; empty-state rendering at lines 110-111.
- Trigger / reproduction: Open a pet detail page and reject its vaccination request while the pet detail request succeeds.
- Expected and observed behavior: The panel should indicate that vaccination data could not be loaded. The catch instead sets `upcoming=[]`, displaying the same no-plans message as a successful empty result.
- User or system impact: A network/server error falsely tells the owner that no vaccinations are planned. This panel provides neither an error indication nor a retry control.
- Evidence and confidence: High. Reviewer executed the unchanged component in a separate in-memory hook/API harness, rejected getList, and asserted that the resulting tree contains the Japanese no-plans message. An initial combined harness assertion had a shell encoding problem; the independent rerun used Unicode escapes and passed.
- Suggested correction and regression check: Keep a distinct failed-load state with an error and retry action. Reserve the empty message for a successful empty list. Test rejected, empty-success and populated-success responses.
- Disposition: open.

### R-03 [P2] An old reminder can suppress the rescheduled vaccination's reminder

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/application/vaccination/service/SendVaccinationRemindersService.java:28-35`; related predicate in `infrastructure/persistence/vaccination/VaccinationJpaRepository.java:63` and timestamp in `domain/notification/model/Notification.java:28-30` (under the same Java package root).
- Trigger / reproduction: (1) Scheduler selects vaccination V with date D1 into its target list. (2) Before V's notification is constructed, the owner cancels V and reschedules to D2; both transactions commit, setting updated_at=t2. (3) Scheduler constructs the cached D1 notification at t3 > t2 and commits. This can occur with one scheduler instance while it processes its target list.
- Expected and observed behavior: docs/07 section 6-10 and docs/08 section 11-1 promise a reminder for the new schedule. The saved notification contains D1, but created_at >= updated_at makes later queries treat D2 as already notified. Even when D2 later enters the seven-day window, no correct reminder is generated unless the record changes again.
- User or system impact: The owner receives an obsolete date and loses the reminder for the new date. This exceeds the disclosed benign completion/reminder race and does not require multiple instances.
- Evidence and confidence: High from source tracing. Selection is an unlocked projection, no schedule revision is carried to notification persistence, and forVaccination timestamps at construction. Existing H2 tests cover notifications created before rescheduling, not this reverse ordering. No PostgreSQL reproduction was executed because Docker access was denied.
- Suggested correction and regression check: Serialize target revalidation and notification insertion with vaccination transitions, or tie deduplication to the exact schedule revision selected. Add a deterministic check that pauses after extraction, commits cancel/reschedule, resumes reminder processing, and verifies that D2 still gets its own correct reminder.
- Disposition: open.

### R-04 [P3] Screen copy promises daily reminders instead of one per schedule revision

- File and line: `frontend/src/pages/VaccinationPage.tsx:113`; `docs/05_Screen_Design 362d097bd4f7806e9e80cf6e5a3f5020.md:407`.
- Trigger / reproduction: Register a vaccination seven days away and leave it unchanged after its first reminder.
- Expected and observed behavior: Page and screen design say notifications arrive every morning at nine starting seven days before the date. The query excludes the vaccination after the first notification, as explicitly required by docs/07 section 10 and docs/08 section 11-1.
- User or system impact: Owners are promised follow-up reminders that will not arrive.
- Evidence and confidence: High; direct comparison of copy, documented deduplication and tested query predicate.
- Suggested correction and regression check: Describe the daily eligibility check and one reminder per schedule, including rescheduling, consistently in the page and screen design. Keep deduplication. Check copy against a second unchanged scheduler run producing zero notifications.
- Disposition: open; minor copy/document correction.

## Verification

Reviewer runs used the snapshot above, Windows, JDK 17.0.19 and Gradle 8.14.4. No application source, product docs, task, handoff or scripts were edited.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: `sha256sum -c .ai-collab/tasks/2026-10-05-vaccination/HANDOFF-001-SHA256.txt`; branch/HEAD/status | Exit 0; all 52 entries OK; branch/base match | None needed | Unrelated untracked .vscode/ and spring-api/.claude/ excluded |
| AC-01..05,07,08 | spring-api/: `./gradlew.bat --offline --no-daemon clean test` | Exit 0; BUILD SUCCESSFUL; 49 XML suites, 253 tests, 0 failures/errors/skips | Handoff also reported 253 | H2/mocks do not prove PostgreSQL concurrency |
| Test environment | Initial `./gradlew.bat clean test` | Exit 1 before tests: default C:\.gradle wrapper lock directory inaccessible | None | Resolved by copying existing wrapper/caches into spring-api/.gradle/review-home and setting GRADLE_USER_HOME there; full successful run above |
| AC-09 build | frontend/: `npm.cmd run build` | Exit 0; TypeScript and Vite passed, 135 modules | Handoff browser flows | Initial npm run build blocked by PowerShell npm.ps1 policy; .cmd executes the same package script |
| AC-09 stale/error handling | frontend/: inline Node + installed TypeScript transpiler; unchanged TSX with mocked hooks/router/API | R-01 and R-02 reproduced in memory | Handoff browser checks | No browser/native run; harness is not React DOM integration |
| JST | Root: inline Node execution of transpiled frontend/src/utils/date.ts; JVM/scheduler source trace | Both sides of 15:00 UTC midnight passed; page uses shared JST helper; scheduler uses Asia/Tokyo; main sets JVM zone | None needed | Actual cron firing not executed |
| AC-06 and real SQL races | Root: docker version; inspected all three supplied scripts | Exit 1: Docker engine named-pipe permission denied; no database created or server started; no access to daipetto or port 8080 | Handoff PostgreSQL migration/dedupe and 13/13 HTTP/race checks | Not independently rerun; no scratch DB exists to drop |
| Authorization/state updates | SecurityConfig, Long principal extraction, pet lookup/ownership, all endpoints and update predicates | Ownership enforced through non-deleted pet lookup; transitions use expected status/deleted predicate; delete preserves status; zero-row errors match docs | Handoff cross-user HTTP checks | WebMvc disables filters; no live auth run |
| Architecture/docs | Changed files and surrounding code vs docs/02,05,06,07,08,11, TODO, AGENTS; pet-fields migration tree | V11 table/FK/indexes and API/state/error mappings agree; normal cancel/delete scope reflected; R-04 discrepancy | Historical test entries remain historical | Existing pet-fields V4 collision documented; not merged |

MapStruct emitted unmapped-method warnings for vaccination complete/reschedule and existing hospital suspend; mapping tests passed. No demonstrated mapping defect was found.

## Remaining risks and next action

- Claude Code should address R-01 through R-03 and synchronize the R-04 copy/doc correction, then publish HANDOFF-002 with a new frozen manifest and regression evidence. Preserve this review and HANDOFF-001.
- PostgreSQL migrations/forced races, browser flows, Android/iOS and cron triggering remain reported-only or unverified as shown above. Required local test/build commands passed independently.
- Known handoff limits remain: single scheduler instance, past scheduled dates allowed, and concurrent completion may yield an obsolete notification. R-03 is a separate loss of a rescheduled reminder.
- No integration, commit, push, source fix or product-doc edit occurred. REVIEW-001.md is the sole review deliverable; test/build outputs are generated verification artifacts.
- Verification cache cleanup was rejected by automatic approval review as blocked by policy (no further reason supplied). The generated spring-api/.gradle/review-home cache remains; application sources are unchanged.
