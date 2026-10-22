# Review 002: 2026-10-21-user-management

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22 (Asia/Seoul)
- Consumed handoff: HANDOFF-002.md; previous findings: REVIEW-001.md
- Reviewed source: `feat/spring/user-management`, base/HEAD `7ac79e380ce99287086964f2cc103648a03b3c56`, uncommitted working-tree snapshot.
- Snapshot verification: **38/38** entries in `HANDOFF-002-SHA256.txt` matched from the repository root before review and again after verification. The patch matches the current scoped `git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend` after newline normalization. Patch SHA-256: `ac8f090baebd929ddf95b2f5f054c3fe5d40ba3d782cad0ad197ff03a6d9a274`.
- Verdict: **ACCEPTED**
- Scope and criteria checked: R-01..R-03 against their original triggers; login/session-lock/projection path, suspension and refresh interaction, frontend response handling and pagination, canonical status and related security/test documentation. Connected admin search/status/role, authorization and error-mapping code was inspected for regressions. Assumed the documented MVP workload including concurrent requests. This is a focused re-review, not a full-repository audit.

## Findings

No new actionable findings in the inspected scope.

### R-01 [P2] Login overlapping suspension — resolved

- File and line: `spring-api/src/main/java/koh/portfolio/springapi/application/auth/service/LoginService.java:53-62`; `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/persistence/user/UserJpaRepository.java:33-34`; `UserPersistenceAdapter.java:52-53` in the same directory.
- Original trigger: login reads ACTIVE, suspension commits before login acquires the session lock, then login resumes.
- Resolution: login obtains the row lock and reads `select u.status` before token revocation/insertion. This scalar projection bypasses the previously loaded entity's status. A non-ACTIVE result raises AUTH-002, with no token saved or successful response returned. Token strings are constructed earlier but neither returned nor persisted on failure. The transaction retains the lock through the write, covering the opposite ordering too.
- Evidence: reviewer-executed `LoginServiceTest.login_fail_when_suspended_after_first_read` checks AUTH-002, lock-before-read and no revoke/save; the success test checks lock/read/revoke/save order. Reviewer-executed H2 `UserPersistenceAdapterTest.find_status_by_id_bypasses_cached_entity` proves a cached ACTIVE entity does not mask the updated SUSPENDED column; missing ID returns null. The port delegates to that projection and login rejects null too.
- Regression review: suspension uses the same lock. Refresh's conditional revoke after acquiring that lock prevents a token revoked by preceding suspension from producing a successor. No isolation override was found in the searched source. The new PostgreSQL script coordinates the original login interleaving by observing the blocked lock request before committing suspension, then asserts 403 AUTH-002 and zero active refresh tokens. Execution is **reported-only**.
- Disposition: **resolved**. High confidence from source and executed unit/H2 checks; no reviewer-executed PostgreSQL race.

### R-02 [P2] Last filtered row leaves an inaccessible empty page — resolved

- File and line: `frontend/src/pages/admin/AdminUserPage.tsx:46-65`.
- Original trigger: page index 1 contains the last of 21 matches; suspension or role change removes it, leaving 20 matches on page 0.
- Resolution: an empty out-of-range response moves `page` to `max(0, totalPages - 1)` without publishing the empty response. The page dependency triggers a refetch. The active-request guard covers this branch. Page 0 with zero matches remains a valid empty result.
- Evidence: reviewer transpiled the actual TSX in memory and executed it with stubbed React state/effects and API responses. ACTIVE and ROLE_USER filter scenarios requested `[1, 0]` and ended with 20 rows while preserving filters. Three additional cases passed: zero matches at page 0 (one request), an out-of-range page becoming genuinely empty (`[1, 0]`, zero rows), and multiple pages shrinking (`[4, 1]`). This verifies effect/state behavior, not browser or real HTTP mutations. The first harness invocation had a harness syntax error; after correcting the in-memory harness, all five cases passed. No source/test file was added.
- Disposition: **resolved**. Implementer browser evidence remains reported-only.

### R-03 [P3] Canonical status contradicts implemented user management — resolved

- File and line: `AGENTS.md:172`, `:186`, `:188`.
- Resolution and evidence: Not started now lists schedule generation, hospital affiliation/admin reservation work and Django without user management. The stack includes `feat/spring/user-management` marked in review, matching the checkout and handoff boundary. The implementation paragraph is consistent, and line 151's test count agrees with the fresh run.
- Disposition: **resolved**. Coordinator-owned status can be updated after this acceptance at the next handoff boundary.

## Verification

Reviewer commands used the verified snapshot above on Windows, JDK **17.0.19**, Node **24.17.0**.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Target integrity | Root: SHA-256 for every manifest entry; scoped diff comparison; branch/HEAD/status | 38/38 match twice; patch matches after newline normalization | None needed | Unrelated `.vscode/` and `spring-api/.claude/` excluded. `frontend/src/api/client.ts` has no textual HEAD diff. |
| Spring suite | `spring-api/`: `$env:GRADLE_USER_HOME='E:/workspace/daipetto/spring-api/.gradle/review-home'; ./gradlew.bat clean test --offline` | **Exit 0, BUILD SUCCESSFUL; 281 tests, 51 suites, 0 failures/errors/skips**, from fresh UTF-8 XML reports | Handoff reports 281 passing | H2/mocks do not establish PostgreSQL concurrency. Two existing MapStruct warnings. |
| R-01 | Source trace and regression tests above, included in full suite | Passing ordering/failure and H2 cache-bypass checks | PostgreSQL login race: 403 AUTH-002, zero active tokens; acceptance 15/15 and auth regression 12/12 | Scripts inspected; no reviewer PostgreSQL execution. |
| R-02 | `frontend/`: `npm.cmd run build`; Node stdin TSX harness using `typescript.transpileModule`, `vm.runInNewContext` and stubbed hooks/API | **Exit 0** build, 136 Vite modules; corrected harness **exit 0**, five cases pass | Browser 21-to-20 ACTIVE filter transition | No independent browser/native run; harness simulates post-mutation responses. |
| R-03/docs | AGENTS status; docs 09 security and 11 test-strategy diffs | Status corrected; lock/projection and ADMIN-T007/T008 documented consistently | None needed | Historical delivery claims outside this re-review not revalidated. |
| PostgreSQL availability | Root: `docker info --format '{{.ServerVersion}}'` | **Exit 1**, Docker config/named-pipe permission denied | All PostgreSQL results are **reported-only** | Docker unavailable to reviewer. |
| Cleanup | `spring-api/`, same Gradle home: `./gradlew.bat --stop`; root: `Test-Path spring-api/.gradle/review-home` | Exit 0, one daemon stopped; directory exists | None | Review-home retained. |

## Remaining risks and next action

No previous finding remains open and no new required correction was found. PostgreSQL concurrency and live browser evidence remain implementer-reported; the reviewer independently verified regression logic, H2 cache bypass and simulated frontend state transitions.

Known limitations remain unchanged: a DB lookup per authenticated request, stale SPA role/menu after a remote role change, and no guard against removing the last other SYSTEM_ADMIN. Hospital affiliation/admin reservation work remains outside this re-review.

Claude Code, as coordinator, can record acceptance for this snapshot and proceed with the user-authorized delivery workflow. Acceptance does not itself merge, push or deploy anything. Only this review was authored; verification generated normal build/cache output. `spring-api/.gradle/review-home` was not deleted. REVIEW-001 and both handoffs remain untouched.
