# Review 002: HANDOFF-002

- Reviewer: Codex, with separate read-only auth and reservation reviewers.
- Publication: final.
- Date: 2026-09-14, Asia/Seoul.
- Verdict: CHANGES_REQUESTED.
- Consumed handoff: [HANDOFF-002.md](HANDOFF-002.md).
- Source: feature/reservation-reject, HEAD 5bbf885295b3c584e6e7d4cbcd524a083c677b54 plus handoff working-tree changes.
- Identity check: all 39 entries in HANDOFF-002-SHA256.txt matched before and after review. No application or test source changes were made.

## Required correction

### R-01 [P1] Partial fix: refresh still races with login and logout

Primary location: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/persistence/auth/RefreshTokenJpaRepository.java:14–18`.

Callers: `application/auth/service/LoginService.java:51`, `LogoutService.java:18`; successor creation: `RefreshTokenService.java:65–88`, all under `spring-api/src/main/java/koh/portfolio/springapi/`.

The conditional revoke count correctly rejects two refresh requests consuming the same token. However, this does not serialize refresh with the per-user bulk revoke used by login and logout. The residual is broader than the handoff's acknowledged login-vs-login case:

1. Refresh transaction A changes old token R to revoked and retains its row lock.
2. Login/logout transaction B starts its bulk `UPDATE ... WHERE user_id=1 AND revoked=false`, sees the older R version and waits for A.
3. A inserts successor R2 and commits.
4. B rechecks R, now revoked, and skips it. R2 was not visible in B's statement snapshot and is not revoked.
5. Logout returns success while R2 remains active. Login inserts L2, leaving both R2 and L2 active.

This is still a security/session-consistency defect, not only an optional choice about rejecting simultaneous logins. A per-user unique active-token index alone would not resolve the logout path. Use a common per-user serialization mechanism across login, refresh, and logout, such as locking the same existing user row before token operations. A session-generation mechanism is another option if its checks cover issuance and revocation consistently.

Required regression coverage: arrange for refresh to acquire its lock first, then start logout/login while refresh is paused before successor insertion. Verify no successor survives the logout ordering and the login path retains the defined one-active-session invariant. Also retain concurrent refresh and concurrent login coverage.

### Independent PostgreSQL reproduction

Executed on the running PostgreSQL 16.9 container in a new, isolated database, using a minimal table containing token/user_id/revoked and the same update predicates. Session A paused four seconds after its conditional revoke. Session B started during the pause. This tests the database mechanism, not Spring HTTP endpoints.

```sql
-- Initial state: ('old', 1, false)
-- Session A: refresh
BEGIN;
UPDATE refresh_tokens SET revoked=true WHERE token='old' AND revoked=false;
SELECT pg_sleep(4);
INSERT INTO refresh_tokens VALUES ('refresh-successor',1,false);
COMMIT;

-- Session B: logout, started while A is sleeping
BEGIN;
UPDATE refresh_tokens SET revoked=true WHERE user_id=1 AND revoked=false;
COMMIT;

-- In a second run, B models login by inserting before COMMIT:
-- INSERT INTO refresh_tokens VALUES ('login-successor',1,false);
```

Observed results:

| Scenario | A conditional update | B bulk update | Active tokens after both commits |
|---|---|---|---|
| refresh + logout | UPDATE 1 | UPDATE 0 | refresh-successor |
| refresh + login | UPDATE 1 | UPDATE 0 | login-successor, refresh-successor |

The scratch database `codex_r01_review_20260914_2013` was dropped successfully. The existing development database was not changed. Initial connection using the documented `daipetto` role failed before creation; the actual container role `daipetto_user` was then used. No passwords were read or recorded.

## Per-finding disposition

| ID | Disposition | Review evidence |
|---|---|---|
| R-01 | PARTIALLY RESOLVED, P1 remains open | Atomic same-token consumption works; cross-operation race reproduced above |
| R-02 | RESOLVED | Per-issuance UUID jti removes same-second collision; real-provider regression test passes |
| R-03 | RESOLVED for original scenario, requires V8 deployment | Partial unique index enforces one active reservation; saveAndFlush exposes violation within adapter |
| R-04 | RESOLVED | All four transitions use transactional conditional updates against expected status; zero rows becomes RESERVATION-008 |
| R-05 | RESOLVED for already-suspended hospital | Creation now requires ACTIVE and returns RESERVATION-009 otherwise |
| R-06 | RESOLVED | History uses deletion-inclusive pet lookup while reservation ownership checks remain |
| R-07 | RESOLVED for refresh-as-Bearer exception | Access-purpose and role checks prevent null authority; real filter/provider tests pass |
| R-08 | RESOLVED | Queue resolves/rejects all waiters and sets retry flags; independently rerun success and failure scenarios |

## Nonblocking observations and boundaries

- `ReservationPersistenceAdapter.java:31–32` maps every DataIntegrityViolationException to RESERVATION-001. Selecting the named unique constraint would be more precise. No current ordinary request path causing another integrity violation was established, so this is not elevated to a new blocking finding.
- R-05 does not serialize a reservation already in progress with hospital suspension. Define the ordering policy if a strict concurrent cutoff is required; the original post-suspension request defect is fixed.
- Existing access tokens without the new type claim are rejected after this change. Existing refresh tokens can still be exchanged and the current frontend can recover through refresh. Document/test this rollout behavior; it is not classified as a blocker here.
- V8 must actually be applied, and pre-existing duplicates must be resolved first. The handoff reports applying V1–V8 and testing concurrency in a scratch database. This reviewer inspected the migration but did not repeat its database application.
- Filter tests use MockFilterChain; they demonstrate the original exception is removed, but do not independently prove the complete HTTP 401/ApiResponse contract.
- Concrete JwtProvider dependencies and reservation-history query count remain previous nonblocking architecture observations, not newly introduced regressions.

## Reviewer-executed checks

| Check | Directory/environment | Result |
|---|---|---|
| `./gradlew.bat clean test` | spring-api, authorized existing Gradle cache | Exit 0; 157 tests, 0 failures/errors/skips |
| `npm run build` | frontend | Exit 0; TypeScript + Vite production build passed |
| Actual client.ts transpiled in memory with mocked transport/store | frontend, Node + existing TypeScript | Failure: both requests rejected. Success: 2 retries, both marked _retry |
| Interleaved refresh vs logout/login SQL | Isolated PostgreSQL 16.9 database | Reproduced R-01 residual; scratch DB removed |
| Handoff hashes | 39 files | All match, including final check |

MapStruct still emits the previously noted `suspend` warning. No new corruption was established. Browser/mobile E2E and full-stack concurrent HTTP tests were not run. Handoff's original PostgreSQL transcripts are implementer evidence; only the R-01 experiments above were independently executed in this round.

## Next action

Claude Code should address R-01 across all three authentication operations and publish HANDOFF-003 with source identity and controlled concurrent-test evidence. No fixes were applied or sent to another task automatically. Re-review is required before VERIFIED.
