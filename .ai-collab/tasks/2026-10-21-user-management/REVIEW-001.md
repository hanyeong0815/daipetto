# Review 001: 2026-10-21-user-management

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22 (Asia/Seoul)
- Consumed handoff: HANDOFF-001.md
- Reviewed source: `feat/spring/user-management`, base/HEAD `7ac79e380ce99287086964f2cc103648a03b3c56`, uncommitted `git diff HEAD` snapshot.
- Snapshot verification: all **36/36** entries in `HANDOFF-001-SHA256.txt` matched, including scripts and patch. The patch also matches the current scoped diff after CRLF normalization. Patch SHA-256: `df05cebb0e69e286441da707053fecabfb9d944e31a90454122e4223994e9072`.
- Verdict: **CHANGES_REQUESTED**
- Scope and criteria checked: AC-01 through AC-06; admin authorization, self rule, conditional writes, session serialization, search/count and paging, filter effects across protected endpoints, input error mapping, frontend callers/routes, docs 02/05/07/08/09/11/12, TODO.md and AGENTS.md. Assumed the documented portfolio/MVP workload, including concurrent requests.

## Findings

### R-01 [P2] A login already in progress can issue a refresh token after suspension commits

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/application/user/service/ChangeUserStatusService.java:27-39`; connected caller `spring-api/src/main/java/koh/portfolio/springapi/application/auth/service/LoginService.java:30-62`.
- Trigger / reproduction: Login reads an ACTIVE user and passes the status check at lines 30-36. Before login reaches its session lock at line 52 (password verification occurs in between), suspension acquires the user lock, sets SUSPENDED, revokes tokens and commits. Login then acquires the same lock and inserts a new, unrevoked refresh token at line 62 without checking the current status.
- Expected and observed behavior: The suspension contract says no new session token survives suspension. This interleaving instead returns a successful login and leaves a refresh token after revocation has completed. `lockForSessionUpdate` returns no refreshed domain user; taking the lock alone does not invalidate the previously read status.
- User or system impact: While SUSPENDED, the new filter still rejects protected API requests, and refresh checks status; this is **not** an immediate protected-API bypass. However, suspension fails to terminate all overlapping sessions. After activation, the surviving refresh token can resume the session without the fresh login described in docs/07 section 11-5.
- Evidence and confidence: High-confidence source-level interleaving, **not a reviewer-executed PostgreSQL race**. The login test checks lock-before-token-write, not status-after-lock. The handoff race script tests an in-flight refresh holding the lock before suspension; it does not test login reading ACTIVE before suspension wins the lock. The old login implementation becomes relevant to this newly added suspension operation.
- Suggested correction and regression check: Validate a fresh user status while holding the session lock before issuing tokens. Account for the already-loaded JPA entity: merely calling `findById` again may reuse the persistence context. Add a coordinated regression where login pauses after reading ACTIVE, suspension commits, and login resumes; expect AUTH-002 and no unrevoked refresh token. Retain the existing refresh-versus-suspension check.
- Disposition: open; required fix.

### R-02 [P2] Changing the last filtered row can strand the list on an empty page

- File and line: `frontend/src/pages/admin/AdminUserPage.tsx:46-49`, `:75-77`, `:156-157`, `:211-217`.
- Trigger / reproduction: Filter ACTIVE users with 21 matches and open page index 1 (20 per page). Suspend its only user. Reload keeps `page=1`; the API correctly returns `content=[]`, `totalElements=20`, `totalPages=1`, `number=1`. Changing the last row's role out of the selected role filter has the same effect.
- Expected and observed behavior: The screen should fetch the last valid page or retain a way back. Instead, it shows both 20 matches and the no-users message. All paging controls disappear because `totalPages > 1` is false.
- User or system impact: The remaining matching users become inaccessible through pagination until the administrator resets a filter/search or reopens the screen.
- Evidence and confidence: Reviewer executed the actual TSX component after in-memory TypeScript transpilation with stubbed React hooks for the response above. Output contained `該当 20 名` and `該当するユーザーはいません。`; both previous and next buttons were absent. This checks the render branch, not a live browser or HTTP interaction. Source inspection confirms reload never adjusts `page`.
- Suggested correction and regression check: When a successful response places the current page beyond the last page, move to `max(0, totalPages - 1)` and refetch, or reset the page after an action. Verify the 21-to-20 transition for status and role filters, and the genuinely empty result case.
- Disposition: open; required fix.

### R-03 [P3] The canonical status still says user management has not been built

- File and line: `AGENTS.md:186` (also the stack listing at `:188`).
- Trigger / reproduction: The new user-management paragraph at line 172 describes the implemented APIs and screen, but Not started still says the admin user screen's backend is not built. The unmerged stack omits the current user-management branch.
- Expected and observed behavior: Canonical status should agree with this snapshot and TODO.md's completed AdminUserPage entry; it currently contradicts them.
- User or system impact: A later session can plan already completed work or use the wrong branch-stack summary.
- Evidence and confidence: Direct comparison of frozen AGENTS.md, TODO.md and current branch. Other reviewed design-doc additions agree with the implemented endpoints, errors, filters and security behavior, subject to R-01.
- Suggested correction and regression check: Remove user management from Not started and extend the stack description. Keep review status pending/changes requested until the new handoff is accepted.
- Disposition: open; minor documentation correction in the next handoff.

## Verification

Reviewer commands used the source revision and dirty snapshot above on Windows, JDK **17.0.19**, Node **24.17.0**.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Target integrity | Root: `Get-FileHash -Algorithm SHA256` for every entry; compare patch with `git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend` | 36/36 match; scoped patch matches after CRLF normalization | None needed | Unrelated `.vscode/` and `spring-api/.claude/` excluded. `api/client.ts` appears in status but has no textual diff against HEAD. |
| Spring initial attempt | `spring-api/`: `./gradlew.bat clean test --offline` | Exit 1: cannot create lock under default `C:\.gradle` | None | Retried below. |
| Spring suite / AC-01..05 | `spring-api/`: `$env:GRADLE_USER_HOME='E:/workspace/daipetto/spring-api/.gradle/review-home'; ./gradlew.bat clean test --offline` | **Exit 0, BUILD SUCCESSFUL; 279 tests, 51 suites, 0 failures/errors/skips**, counted from fresh UTF-8 XML reports | Handoff also reports 279 passing | H2/mocks do not prove PostgreSQL/concurrency. Two existing MapStruct warnings. Review-home retained; its daemon stopped after completion. |
| AC-01 search | Read shared JPQL search/count predicate, adapter paging, DTO mapping, service clamps; H2 suite above | Deleted rows excluded; ID order; password absent from response; keyword CAST present; filters/count aligned | PostgreSQL null-filter, keyword, role and paging HTTP checks reported passing | PostgreSQL binding not independently rerun. |
| AC-02..05 / filter | Trace SecurityConfig, controller authorization declarations, admin services, login/refresh/logout, token writes and JWT filter; suite above | Method security enabled; SYSTEM_ADMIN class guard; Long principal; self checks; column-only writes with deleted-row condition and affected-row check; current DB status/role used across protected endpoints. **R-01 remains.** | HTTP authorization, next-request status/role behavior, refresh-lock race and 12 auth regressions reported passing | WebMvc slices exclude filters and do not independently establish runtime authorization. |
| Input mapping | GlobalExceptionHandler and controller suite | Invalid query enum / JSON enum and missing role return 400 VALIDATION-001 in tests | HTTP enum checks reported passing | No separate full-stack run by reviewer. |
| AC-06 build/callers | `frontend/`: `npm.cmd run build`; inspect all UserProfile/auth-store consumers, login assignment, routes, dashboard and admin APIs | **Exit 0**, TypeScript/Vite pass (136 modules). No remaining `UserProfile.id` consumer; `/users/me` maps `userId` and `status`, stored response is not renamed. Dashboard role visibility matches guards. | Browser actions reported in handoff | Browser/native behavior not independently executed. |
| AC-06 pagination edge | `frontend/`: Node stdin harness using `typescript.transpileModule`, `vm.runInNewContext`, stubbed hooks and JSX traversal of actual AdminUserPage | **Exit 0; R-02 reproduced** for page 1 with 20 remaining matches / one total page | Not covered by reported browser check | Render-only harness; no source/test file added. |
| PostgreSQL availability | Root: `docker info --format '{{.ServerVersion}}'` | **Exit 1**, Docker named-pipe/config access denied | Handoff reports 14/14 PostgreSQL checks and a lock-observed refresh race; scripts inspected | PostgreSQL evidence is **reported-only**. |
| Docs | Scoped diffs and relevant surrounding specs/status | R-03; security contract affected by R-01 | None needed | Historical review claims not revalidated. |

## Remaining risks and next action

Claude Code should fix R-01 and R-02, synchronize affected docs and R-03, and publish **HANDOFF-002** with a new immutable hash manifest. Include the login-versus-suspension regression and pagination evidence. Preserve HANDOFF-001 and this review.

The documented stale SPA role/menu after a remote role change remains a known limitation: backend authorization uses the current role; the stored UI profile updates on login. Hospital affiliation checks and the mock admin reservation screen remain documented exclusions, not new findings here. PostgreSQL and live browser results are implementer-reported only. No commit, merge or deployment was performed. The only authored file is this review; verification generated normal build/cache output and did not delete review-home.
