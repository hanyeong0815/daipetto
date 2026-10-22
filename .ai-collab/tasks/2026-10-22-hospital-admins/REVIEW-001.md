# Review 001: 2026-10-22-hospital-admins

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22 (Asia/Seoul)
- Consumed handoff: HANDOFF-001.md
- Reviewed source: `feat/spring/hospital-admins`, base/HEAD `71c0fe8cb05759338bc6ecd669713394fa6941b0`, uncommitted `git diff HEAD` snapshot.
- Snapshot verification: all 86 entries in HANDOFF-001-SHA256.txt matched SHA-256 from the repository root, including the patch and both scripts. Unrelated untracked `.vscode/` and `spring-api/.claude/` were excluded.
- Verdict: CHANGES_REQUESTED
- Scope and criteria checked: AC-01 through AC-07; migration/entity/adapter, authorization coverage and ordering, assignment versus demotion, reservation query, controllers, SC-013/SC-014 and docs 02/05/06/07/09/11/12, TODO.md and AGENTS.md. Portfolio/MVP load and the documented unpaged list/candidate cap were assumed.

## Findings

### R-01 [P2] A completed suspension can replace the selected hospital with the previous hospital's data

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:150`; related rendering at lines 321 and 366, form synchronization at lines 93-97, and update target at lines 129-132.
- Trigger / reproduction: As SYSTEM_ADMIN, select hospital A, confirm suspension, and switch to hospital B while the PATCH is pending. Let B's detail load, then let A's PATCH complete. The old handler calls `fetchHospitalDetail(A)` even though the selector now holds B.
- Expected and observed behavior: B must remain the displayed and editable hospital. Instead, the store treats this late call for A as its newest request and displays A. The new keyed HospitalAdminSection also switches to A, while the selector and basic-information save target remain B.
- User or system impact: Saving the visible form can overwrite B's name/address/phone with A's values. Admin assignments can also be made to A while the selector says B. Backend affiliation checks cannot prevent this when the actor can manage both hospitals.
- Evidence and confidence: High. Reviewer executed actual `hospitalStore.ts`, transpiled in memory with installed TypeScript, using real Zustand and a stub detail API. Replayed detail(A), select/detail(B), then the suspend continuation's detail(A). Output: `{"selectedHospitalId":2,"renderedHospitalId":1,"formName":"Hospital 1","saveTarget":2}`; exit 0. The page's handler and render paths establish the remaining flow. This was a Node/store reproduction, not a browser run. The new suspend detail refresh introduces this path; the existing save refresh has the same underlying risk.
- Suggested correction and regression check: Guard post-mutation refreshes against the current selection, and render/edit detail only when its hospital ID matches the selected ID. Alternatively, prevent selection changes for the entire mutation/refresh operation. Cover delayed suspend/save for A followed by selection of B; B's detail, form, schedules, admin section and mutation targets must remain consistent.
- Disposition: open; required before acceptance.

### R-02 [P2] Failed managed-hospital loading is displayed as no affiliation and offers no retry

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:75-77`, empty-state rendering at lines 294-298.
- Trigger / reproduction: Open SC-014 with linked hospitals and make the first `GET /admin/hospitals/mine` fail (network interruption or 500). Restore connectivity.
- Expected and observed behavior: Show a failed-load state with a retry. The catch instead converts initial `null` to `[]`, so the error banner appears together with “no managed hospital; ask the system administrator to link you.” SYSTEM_ADMIN receives a misleading instruction to create a hospital. No retry control invokes `fetchHospitals`; a hospital admin must navigate away or reload the page to recover.
- User or system impact: A temporary service failure looks like missing configuration and strands the management screen even after the service recovers.
- Evidence and confidence: High, static trace of the new catch, mount-only fetch effect, empty-state branch and fetch callers. No browser failure injection was executed. SC-013 already distinguishes failed loading from a successful empty result.
- Suggested correction and regression check: Keep failure distinct from a successful empty response and add retry. Verify a rejected initial request shows no no-affiliation/create-hospital message, and retrying a successful response restores the linked hospital and auto-selection.
- Disposition: open; required before acceptance.

### R-03 [P3] Shared test policy still says no PostgreSQL concurrency test exists

- File and line: `AGENTS.md:154`.
- Trigger / reproduction: Compare “No PostgreSQL concurrency test exists yet” with this snapshot's acceptance script HA-12/HA-13 and docs/11's HADM-T005/T008 evidence note.
- Expected and observed behavior: Startup policy should distinguish available manual PostgreSQL checks from absent automated/independently rerun coverage. Its blanket statement now contradicts the added script.
- User or system impact: Future reviews can incorrectly conclude there is no existing concurrency verification path.
- Evidence and confidence: High; the script contains concurrent assignments and an observed user-row lock wait. Its execution remains implementer-reported in this review.
- Suggested correction and regression check: Qualify the statement: manual hospital-affiliation PostgreSQL checks exist; H2/mocks do not prove concurrency, and other race coverage remains separately tracked. Cross-check docs/11 and the handoff without upgrading reported evidence to reviewer-executed evidence.
- Disposition: open, nonblocking documentation correction; include in the next handoff.

## Verification

All checks apply to the dirty snapshot above. Reviewer environment: Windows PowerShell, Microsoft OpenJDK 17.0.19, Node 24.17.0, Gradle wrapper 8.14.4.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: parse manifest and run `Get-FileHash -Algorithm SHA256` for every entry; git status, branch and HEAD | 86/86 matched; branch and HEAD match; exit 0 | None needed | Local dirty snapshot |
| Spring suite | `spring-api/`: `./gradlew.bat clean test --offline` | Default home failed, exit 1: cannot create wrapper lock under `C:\.gradle` | None | Retried below |
| Spring suite | `spring-api/`: set `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home`; `./gradlew.bat clean test --offline` | `BUILD SUCCESSFUL in 59s`; six tasks executed. Fresh UTF-8 XML totals: 301 tests, 0 failures/errors/skips. Tool session did not surface a final shell exit code after success output | Handoff also reports 301 passing | Existing Hospital/Vaccination MapStruct warnings; review-home retained |
| AC-01/04 | V15/entity/mapper/adapter inspection; H2 persistence tests in suite; enumerate V15 paths across local/remote refs | Named pair uniqueness, FKs/index; targeted duplicate translation with saveAndFlush. No committed V15 collision found in available refs | Fresh PostgreSQL Flyway V1-V15 and duplicate HTTP checks | H2 does not execute V15; PostgreSQL not rerun |
| AC-02/03 | Trace all hospital/reservation endpoints and managing services, checker and management queries; service/controller tests | No missing affiliation guard found on hospital-admin mutations or reservation list/transitions. Hospital update, schedule create/block/unblock and hours create/update/delete check before resource validation. Transitions load reservation first, then check before state transition/notification. SYSTEM_ADMIN-only create/suspend/link endpoints correctly rely on role checks. `/mine` excludes deleted hospitals and includes suspended ones | PostgreSQL HTTP role/affiliation checks | JWT-excluded slices do not independently prove deployed method security; documented check-then-act unlink window remains |
| AC-05 | Assignment transaction, user-row lock, target lookup, role UPDATE and link deletion; service tests | Lock precedes target-role read; role UPDATE locks the same row and deletion remains in that transaction. Both serialization orders preserve the intended invariant by inspection | HA-13 lock wait then HOSPITAL-006/zero links; HA-11 demotion/promotion | PostgreSQL race not rerun; HA-13 models demotion with psql |
| AC-06 | JPQL/projection/DTO inspection; ReservationAdminQueryTest and controller/exception tests | H2 passes filters/nulls, joins, deleted reservation exclusion, deleted-pet name retention and ordering. Date null test uses cast; enum predicate has typed comparison. Required hospitalId and missing/bad-param 400 handling checked; assignment returns 201 | HA-06/07 PostgreSQL null filters and HTTP validation | PostgreSQL parameter typing remains reported-only |
| AC-07 build | `frontend/`: `npm.cmd run build` | Exit 0; TypeScript/Vite pass, 136 modules | Browser flows in handoff | No reviewer browser run |
| AC-07 state | Both pages/API types/store; in-memory Node reproduction in R-01 | Store reproduction exit 0 confirms mismatch. SC-013 ignores obsolete list responses; keyed admin section cleans up its own fetch effect but cannot correct its parent's mismatch. R-02 confirmed by source | Happy-path browser and empty-affiliation flows | Delayed mutation and failed-load browser regressions needed |
| PostgreSQL access | Root: `docker version` | Client installed; config access denied and engine pipe permission denied | Handoff: 13/13 acceptance and 12/12 regression | No DB acceptance rerun or scratch DB created |
| Documentation | Relevant diffs/sections of docs 02/05/06/07/09/11/12, TODO.md, AGENTS.md against code | Endpoints, errors, roles, schema and UI substantially synchronized; R-03 remains | PostgreSQL/browser claims retained as implementer evidence | Product docs not edited |

## Remaining risks and next action

- Claude Code should fix R-01/R-02, address R-03, and publish HANDOFF-002 with a new snapshot manifest. Re-review delayed-response and failed-load cases and run checks appropriate to the fixes.
- PostgreSQL and browser acceptance remain reported-only. Mocks/H2 and a production build do not prove production database concurrency or browser behavior.
- Unpaged reservations, first-100 candidates and check-then-act authorization are documented limitations, not new findings. Slot auto-generation is outside this round.
- Only this review record was authored. Application/source/docs were not edited; `spring-api/.gradle/review-home` was not deleted. No commit, push or merge was performed.
