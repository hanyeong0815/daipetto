# Handoff 003: 2026-09-13-implementation-review

- Author / implementer: Claude Code
- **Publication: draft** — the concurrency evidence REVIEW-002 requires has not been executed yet (see Acceptance evidence). Do not start the re-review until this says ready.
- Timestamp: 2026-09-23, Asia/Seoul
- Task specification: TASK.md (coordinator-owned by Codex; not edited here)
- Recipient: Codex
- Requested action: re-review of R-01 once this record is marked ready
- Consumed review: [REVIEW-002.md](REVIEW-002.md)
- Source base SHA: 5bbf885295b3c584e6e7d4cbcd524a083c677b54
- Source head SHA: same — changes remain uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Checkout/branch and dirty state: `feature/reservation-reject`, dirty (47 files)
- Snapshot: `HANDOFF-003.patch` / `HANDOFF-003-SHA256.txt`, captured 2026-09-23. **Provisional**: if the pending evidence run forces a code change, both are recaptured before this record is marked ready.

## Change summary

R-01 is addressed by serializing all three token-mutating operations per user, as REVIEW-002 suggested (locking the existing user row).

| Area | Change |
|---|---|
| `domain/user/port/UserRepository` | new `lockForSessionUpdate(Long userId)` |
| `infrastructure/persistence/user/UserJpaRepository` | `findByIdForUpdate` with `@Lock(PESSIMISTIC_WRITE)` |
| `infrastructure/persistence/user/UserPersistenceAdapter` | implements the port method |
| `application/auth/service/LoginService` | locks the user row before `revokeAllByUserId` |
| `application/auth/service/RefreshTokenService` | locks the user row before the conditional revoke and successor insert |
| `application/auth/service/LogoutService` | now depends on `UserRepository`; locks before `revokeAllByUserId` |

Ordering is identical everywhere (user row first, then `refresh_tokens`), so the three operations cannot deadlock against each other. The conditional revoke from round 002 is kept: it still decides the winner among concurrent refreshes, while the lock is what makes login/logout see a successor inserted by a concurrent refresh.

This also closes the residual deferred in HANDOFF-002 (login vs login): concurrent logins now serialize and leave exactly one active token, without the per-user unique index whose failure mode was rejected there.

### Nonblocking observation from REVIEW-002

`ReservationPersistenceAdapter.save` no longer maps every `DataIntegrityViolationException` to RESERVATION-001. It converts only violations of `uq_reservations_active_schedule` (Hibernate `ConstraintViolationException.getConstraintName()`, falling back to the message) and rethrows anything else unchanged, so a future constraint cannot be disguised as a duplicate reservation.

### Not changed

- R-05 concurrent cutoff: suspension still applies to requests that start after it. Recorded as a policy, not a defect.
- Rollout of the `type` claim: access tokens issued before the change are rejected; refresh tokens still work, so clients recover through one refresh. Documented in docs/09 §3-2.
- MapStruct `suspend` warning and the `JwtProvider` port observation remain untouched architecture notes.

## Acceptance evidence

| Criterion | Test/check | Working directory and command | Exit code/outcome | Evidence location |
|---|---|---|---|---|
| Whole suite | `./gradlew clean test` | `spring-api/` | exit 0 — 161 tests, 0 failures/errors/skips (was 157) | `build/test-results/test/*.xml` |
| Lock precedes revoke (refresh) | `RefreshTokenServiceTest.refresh_token_success` `InOrder` | `spring-api/` | PASS — `lockForSessionUpdate` → `revokeByToken` → `save` | test source |
| Lock precedes revoke (login) | `LoginServiceTest.login_success` `InOrder` | `spring-api/` | PASS | test source |
| Lock precedes revoke (logout) | `LogoutServiceTest.logout_locks_user_before_revoking` | `spring-api/` | PASS | new test file |
| Locking query is valid SQL | `UserPersistenceAdapterTest` (`@DataJpaTest`, H2 PostgreSQL mode) | `spring-api/` | PASS — runs for an existing and a missing id | new test file |
| Narrowed constraint mapping | `ReservationPersistenceAdapterTest.save_rethrows_unrelated_integrity_violation` | `spring-api/` | PASS — unrelated violation propagates unchanged | test source |
| **refresh × logout ordering** | `r01-sql.sh` + `r01-http.mjs` (E1) | isolated PostgreSQL + backend on :18080 | **NOT RUN** | — |
| **refresh × login ordering** | same (E2) | same | **NOT RUN** | — |
| **concurrent refresh / concurrent login** | same (E3, E4) | same | **NOT RUN** | — |
| **each operation waits on the held user row** | same (E5) | same | **NOT RUN** | — |

The last four rows are the coverage REVIEW-002 requires. Both scripts are written and ready: the SQL one replays REVIEW-002's own interleaving against the real schema with and without the lock, and the HTTP one holds the user row from psql so a refresh request takes the lock first and a logout/login request arrives while that refresh is still in flight, then asserts that no session survives logout and that login leaves exactly one active token.

They could not be executed on 2026-09-23: Docker Desktop on this machine does not finish starting (its `docker-desktop` WSL distribution stays `Stopped`, so PostgreSQL is unavailable). Nothing was forced; the environment needs attention from the user first.

## Limitations

- Everything above is unit-level evidence. The per-user lock is proven only in intent (call ordering) and as valid SQL, not yet under real concurrency.
- The suite still contains no automated PostgreSQL concurrency test; the pending runs are manual.
- The earlier E2E evidence for R-02〜R-08 (`HANDOFF-002-E2E.md`) predates this change. The auth paths it exercised are the ones modified here, so the R-01/R-02/R-07 rows should be re-run together with the pending checks.

## Documentation

- `docs/09_Security_Design*` §3-5: per-user serialization of login/refresh/logout and why the bulk revoke alone is unsafe.
- `docs/11_Test_Strategy*`: AUTH-T011〜T013 for the three interleavings.
- `docs/12_Trouble_Shooting*` §3-11: the logout-survives-session symptom and the lock as its fix.
- `AGENTS.md` §3 rule 9, §8 test count, §10 review-round summary.
- `TODO.md`: round recorded, the old login-vs-login residual closed, the pending verification listed.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| R-01 | Fixed in code; verification pending | user-row lock in all three operations + ordering tests | run the four pending concurrency checks, then mark this record ready |
| R-02〜R-08 | Accepted as resolved in REVIEW-002 | — | none |
| Nonblocking: broad integrity-violation mapping | Fixed | constraint-name check + rethrow, with regression test | none |
| Nonblocking: R-05 ordering, token rollout, MapStruct warning, JwtProvider port | Documented, not changed | docs/09 §3-2, this record | user decides if any becomes work |

## Receiver instructions

Wait for this record to say ready. Then reproduce the snapshot by applying `HANDOFF-003.patch` onto 5bbf885 or verifying the working tree against `HANDOFF-003-SHA256.txt`. Source write ownership stays with Claude Code until the pending runs finish.
