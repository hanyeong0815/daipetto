# Handoff 002: 2026-09-13-implementation-review

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-09-13, Asia/Seoul
- Task specification: TASK.md (coordinator-owned by Codex; not edited by this handoff)
- Recipient: Codex
- Requested action: re-review of R-01〜R-08 against this snapshot
- Source base SHA: 5bbf885295b3c584e6e7d4cbcd524a083c677b54
- Source head SHA: same (5bbf885) — all changes are uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Record transport commit: none yet; records and source are local to this checkout
- Checkout/branch and dirty state: `feature/reservation-reject`, dirty (39 changed/added files under AGENTS.md, TODO.md, docs/, spring-api/, frontend/)
- Dirty snapshot manifest and patch: `HANDOFF-002.patch` (1918 lines, `git diff HEAD -- AGENTS.md TODO.md docs spring-api frontend`) and `HANDOFF-002-SHA256.txt` (39 entries) beside this file

Round numbering starts at 002 because REVIEW-001 reviewed pre-existing code with no preceding handoff; 001 is intentionally unused.

## Ownership change

REVIEW-001 recorded "Implementer: none assigned". The user then directly assigned these fixes to Claude Code, which took implementer ownership of all source writes in this round. Codex remains coordinator and reviewer. TASK.md was deliberately left untouched (coordinator-owned); its `Implementer` and `Status` fields still need the coordinator's update.

## Change summary

| Finding | Change | Files |
|---|---|---|
| R-01 | Conditional revoke now returns the affected-row count through port and adapter; only the caller that updates exactly 1 row may issue successors, otherwise AUTH-003 | `RefreshTokenJpaRepository`, `RefreshTokenRepository`, `RefreshTokenPersistenceAdapter`, `RefreshTokenService` |
| R-02 | Refresh tokens carry a per-issuance `jti` (UUID), so same-second reissue cannot collide with the `token` UNIQUE constraint | `JwtProvider` |
| R-03 | New partial unique index `uq_reservations_active_schedule` (V8); adapter uses `saveAndFlush` and translates `DataIntegrityViolationException` to RESERVATION-001 | `V8__add_reservations_active_schedule_unique_index.sql`, `ReservationPersistenceAdapter` |
| R-04 | Transitions persist through `updateStatus(id, expectedStatus, newStatus, updatedAt)` — a conditional UPDATE keyed on the status read earlier; 0 rows means another transition won, mapped to RESERVATION-008. `RejectReservationService` also gained the missing `@Transactional` | `ReservationRepository`, `ReservationJpaRepository`, `ReservationPersistenceAdapter`, Approve/Reject/Complete/Cancel services |
| R-05 | Reservation creation loads the hospital and requires ACTIVE; new code RESERVATION-009 | `CreateReservationService`, `ReservationErrorCode` |
| R-06 | Reservation rendering resolves pets through `findByIdIncludingDeleted`, so a logically deleted pet no longer aborts the whole list | `PetRepository`, `PetPersistenceAdapter`, `GetReservationService` |
| R-07 | Access and refresh tokens are separated by a `type` claim; the filter authenticates only `type=access` tokens carrying a role, leaving other tokens unauthenticated (→ 401 AUTH-001) instead of throwing inside the filter | `JwtProvider`, `JwtAuthenticationFilter` |
| R-08 | Refresh queue stores `reject` alongside `resolve` and settles every waiter on both outcomes; queued retries are marked `_retry` | `frontend/src/api/client.ts` |

Domain state-transition validation was kept: the domain still rejects illegal transitions up front (fast, correct error), and the conditional UPDATE is the concurrency backstop.

## Acceptance evidence

Spring commands run from `spring-api/`; frontend from `frontend/`; SQL scenarios via `docker exec` into the running `daipetto-postgres` (postgres:16.9) container.

| Criterion | Test/check | Working directory and command | Environment | Exit code/outcome | Evidence location |
|---|---|---|---|---|---|
| Whole suite | `./gradlew clean test` | `spring-api/` | JDK 17, H2 test profile | exit 0 — 157 tests, 0 failures/errors/skips (was 140) | `build/test-results/test/*.xml` |
| Frontend build | `npm run build` | `frontend/` | Node 24, Vite 6.4.3 | exit 0 — tsc + production build | terminal output |
| R-01 unit | `RefreshTokenServiceTest.refresh_token_fail_when_already_consumed_concurrently` | `spring-api/` | mocked port | PASS — revoke returns 0 → AUTH-003, no successor saved | test source |
| R-01 SQL | `RefreshTokenPersistenceAdapterTest.revoke_by_token_returns_zero_when_already_revoked` | `spring-api/` | `@DataJpaTest`, H2 PostgreSQL mode | PASS — 1 then 0 affected rows | test source |
| R-01 concurrency | two interleaved psql transactions revoking the same token | container `daipetto-postgres`, throwaway DB | PostgreSQL 16.9 | session A `UPDATE 1`, session B blocked then `UPDATE 0` | reproduced below |
| R-02 | `JwtProviderTest.refresh_tokens_issued_in_same_second_are_unique` | `spring-api/` | real `JwtProvider` | PASS — consecutive tokens differ, both parse to the same userId | test source |
| R-03 constraint | `ReservationPersistenceAdapterTest.save_translates_unique_violation_to_duplicated_error` | `spring-api/` | mocked JPA repository | PASS — `DataIntegrityViolationException` → RESERVATION-001 | test source |
| R-03 migration | applied V1〜V8 to a throwaway database | container `daipetto-postgres` | PostgreSQL 16.9 | V8 applies; `\d reservations` shows `uq_reservations_active_schedule UNIQUE, btree (schedule_id) WHERE status = ANY('REQUESTED','APPROVED') AND deleted_at IS NULL` | reproduced below |
| R-03 concurrency | two interleaved psql transactions inserting REQUESTED for one schedule | container, throwaway DB | PostgreSQL 16.9 | A commits; B fails `duplicate key value violates unique constraint "uq_reservations_active_schedule"`; final active row count = 1 | reproduced below |
| R-04 unit | 4 new `…_fail_when_status_changed_concurrently` tests (approve/reject/complete/cancel) | `spring-api/` | mocked port | PASS — `updateStatus=false` → RESERVATION-008 | `ReservationStateTransitionServiceTest` |
| R-04 concurrency | interleaved approve vs reject on one reservation | container, throwaway DB | PostgreSQL 16.9 | A `UPDATE 1` → APPROVED; B blocked then `UPDATE 0`; terminal state not overwritten | reproduced below |
| R-04 JPQL | `updateStatus` query parsed at context start | `spring-api/` | `SpringApiApplicationTests` (`@SpringBootTest`) | PASS — context loads, query validated | test run |
| R-05 | `CreateReservationServiceTest.create_reservation_fail_when_hospital_suspended` | `spring-api/` | mocked ports | PASS — RESERVATION-009, nothing saved | test source |
| R-06 | `GetReservationServiceTest.get_reservation_list_success_when_pet_deleted` | `spring-api/` | mocked ports | PASS — reservation for a soft-deleted pet still rendered | test source |
| R-07 | `JwtAuthenticationFilterTest` (3 tests) | `spring-api/` | real filter + real `JwtProvider`, `MockFilterChain` | PASS — access token authenticates; refresh token and garbage token leave the context empty without throwing | new test file |
| R-08 | isolated client-logic harness, same approach as REVIEW-001 | `frontend/` | Node + repo TypeScript, stubbed axios/store | pre-fix (`git show HEAD:…/client.ts`): `queued request: PENDING`, exit 1. post-fix: `first: rejected`, `queued: rejected`, exit 0 | harness in session scratchpad |

PostgreSQL scenario transcripts (throwaway database `daipetto_v8_check`, created and dropped inside the existing dev container; the `daipetto` database was not touched):

```text
R-03  A: BEGIN / INSERT 0 1 / pg_sleep / COMMIT
      B: BEGIN / ERROR: duplicate key value violates unique constraint "uq_reservations_active_schedule"
         DETAIL: Key (schedule_id)=(1) already exists. / ROLLBACK
      SELECT count(*) ... active_rows = 1

R-04  A: BEGIN / UPDATE 1 (REQUESTED→APPROVED) / pg_sleep / COMMIT
      B: BEGIN / UPDATE 0 (REQUESTED→REJECTED) / COMMIT
      final: id=1, status=APPROVED

R-01  A: BEGIN / UPDATE 1 (revoked=false → true) / pg_sleep / COMMIT
      B: BEGIN / UPDATE 0 / COMMIT
```

## Limitations

- The PostgreSQL scenarios exercise the **database mechanism** (row locks, partial unique index, conditional-UPDATE row counts) through psql. They do not drive the Spring stack over HTTP, so the mapping from those outcomes to AUTH-003 / RESERVATION-001 / RESERVATION-008 rests on the unit tests listed above, not on an end-to-end concurrent run.
- **R-01 residual — concurrent login vs login is not fixed.** Two simultaneous logins for one user can still both insert an active refresh token, so the "max 1 active per user" invariant can be violated on that path. A partial unique index on `refresh_tokens(user_id) WHERE revoked = false` would close it, but it would turn a legitimate double login into a constraint failure, so the tradeoff is left open for the user rather than decided here. The rotation path (the replay/one-time-use property) is fixed.
- R-02 has no clock-pinned test; two consecutive calls land in the same second in practice, but the guarantee comes from the UUID `jti` rather than from the assertion's timing.
- V8 will fail on any existing database that already holds two active reservations for one schedule; such rows must be cleaned before applying. The local dev `daipetto` database has **not** been migrated in this round.
- The H2 test profile disables Flyway, so no test replays V8. The index is only exercised by the throwaway-database run above.
- Browser/mobile E2E was not run; R-08 evidence is the isolated client-logic harness.
- Not actioned, by scope: the architecture note about `LoginService`/`RefreshTokenService` importing the concrete `JwtProvider` (a token-issuance port) and the reservation-history N+1/pagination note. Both were presented as structural observations rather than findings.
- MapStruct `suspend` warning: inspected, not changed. `HospitalMapper` maps through the all-args constructor and `Hospital.suspend(HospitalStatus)` is never invoked as a setter, so the warning is noise rather than field corruption. Renaming the domain method would silence it.

## Documentation

Synchronized per the docs-sync procedure (TODO.md → docs/ → AGENTS.md):

- `TODO.md` — review round recorded with per-finding disposition and the two remaining verification gaps.
- `docs/06_ERD*` — §12 business constraints note the DB-level guarantee and the SUSPENDED-hospital rule; §17 lists the partial unique index; Updated 2026-09-13.
- `docs/07_API_Design*` — §9-3 adds RESERVATION-009 and the constraint-translation note; §9-4〜9-7 state that a losing concurrent transition returns RESERVATION-008.
- `docs/09_Security_Design*` — §3-2 documents the claim layout (`type`, `jti`) and filter behavior; §3-5 documents single-consumption rotation; Updated 2026-09-13.
- `docs/11_Test_Strategy*` — AUTH-T008〜T010, RSV-T007〜T008, STATE-T009〜T010; Updated 2026-09-13.
- `docs/12_Trouble_Shooting*` — new §3-11 "モックテストを通過する競合状態" covering all four races, their cause, the fixes, and the remaining gap; Updated 2026-09-13.
- `AGENTS.md` — §5 RESERVATION-009, §7 V8 + next number V9 + the pre-existing-duplicates warning, §8 corrected the stale "H2 not introduced" claim and the test count (157), §10 review-round summary.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| R-01 | Fixed (rotation path); residual deferred (login vs login) | conditional revoke count + PostgreSQL transcript | user decides on the per-user active-token constraint tradeoff |
| R-02 | Fixed | `jti` per issuance; `JwtProviderTest` | none |
| R-03 | Fixed | V8 partial unique index + error translation; PostgreSQL transcript | apply V8 to existing databases after cleaning duplicates |
| R-04 | Fixed | expected-status conditional UPDATE; PostgreSQL transcript; 4 new unit tests | none |
| R-05 | Fixed | hospital ACTIVE check + RESERVATION-009 | none |
| R-06 | Fixed | `findByIdIncludingDeleted` + list test | none |
| R-07 | Fixed | `type` claim + `validateAccessToken`; `JwtAuthenticationFilterTest` | none |
| R-08 | Fixed | queue settles all waiters; before/after harness run | none |

## Receiver instructions

Reproduce this snapshot by applying `HANDOFF-002.patch` onto 5bbf885, or by verifying the working tree against `HANDOFF-002-SHA256.txt`. Source write ownership returns to no one at the end of this round; the reviewer should not edit application code. Suggested focus for re-review: whether the conditional-UPDATE approach fully covers the transition matrix, whether translating `DataIntegrityViolationException` to RESERVATION-001 is too broad for future constraints on `reservations`, and whether the deferred login-vs-login residual is acceptable.
