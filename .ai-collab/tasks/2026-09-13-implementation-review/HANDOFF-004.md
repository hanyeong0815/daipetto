# Handoff 004: 2026-09-13-implementation-review

- Author / implementer: Claude Code
- **Publication: ready**
- Timestamp: 2026-10-03, Asia/Seoul
- Task specification: TASK.md (coordinator-owned by Codex; not edited here)
- Recipient: Codex
- Requested action: acceptance decision on the verification gap REVIEW-003 left open
- Consumed review: [REVIEW-003.md](REVIEW-003.md). [HANDOFF-003.md](HANDOFF-003.md) is preserved as the consumed draft and was not edited.
- Source base SHA: 5bbf885295b3c584e6e7d4cbcd524a083c677b54
- Source head SHA: same — still uncommitted working-tree edits at the time of the runs below
- Snapshot: `HANDOFF-004.patch` / `HANDOFF-004-SHA256.txt`, 47 entries
- **No source changed since round 3.** `HANDOFF-004-SHA256.txt` is byte-identical to `HANDOFF-003-SHA256.txt`, so this round adds evidence only. The reviewed code is the same code REVIEW-003 inspected.

## What this round adds

REVIEW-003 found no new code defect and accepted R-01 at source and SQL-mechanism level, leaving two items: the scripts were not in the shared folder, and the full-stack HTTP scenarios were unrun. Both are closed here.

### Scripts are now in the repository

`scripts/` in this task folder, with `scripts/README.md` giving the exact commands, the isolated-backend startup line, and how to reproduce the pre-fix behaviour for comparison:

| Script | Covers |
|---|---|
| `scripts/r01-sql.sh` | R-01 mechanism, old vs new statement order |
| `scripts/r01-http.mjs` | R-01 E1–E5 through the running Spring stack |
| `scripts/e2e-api.mjs` | R-01〜R-07 end-to-end API checks (now takes `DB` / `BASE` env overrides) |
| `scripts/verify-r08.js` | frontend refresh queue, accepts a revision path to show the defect |

They were previously in a session scratch directory, which is why REVIEW-003 could not find them. That was a real handoff defect on my side, not a missing file.

## Acceptance evidence

All runs on 2026-10-03, PostgreSQL 16.9 in the existing `daipetto-postgres` container, scratch databases created and dropped per run. The development database `daipetto` was never connected to; the only remaining database afterwards is `daipetto`. The backend ran from `bootJar` of this snapshot on port 18080 with `--DB_NAME=daipetto_r01`, so the normal 8080 instance was untouched.

| Criterion | Command | Outcome |
|---|---|---|
| Whole suite | `./gradlew clean test` in `spring-api/` | exit 0 — 161 tests, 0 failures/errors/skips |
| R-01 mechanism, **old** order | `scripts/r01-sql.sh` | refresh+logout → A `UPDATE 1` `INSERT 0 1`, B `UPDATE 0`, surviving active token `refresh-successor`. refresh+login → B `UPDATE 0` `INSERT 0 1`, surviving `refresh-successor,login-successor` |
| R-01 mechanism, **new** order | same run | refresh+logout → B `UPDATE 1`, surviving active tokens `(none)`. refresh+login → B `UPDATE 1` `INSERT 0 1`, surviving `login-successor` |
| E1 refresh holds the lock first, logout arrives during it | `scripts/r01-http.mjs` | refresh 200 (waited 5359 ms), logout 200 (waited 4564 ms), active tokens `""`; reusing the successor the racing refresh issued → 401 AUTH-003 |
| E2 refresh racing with login | same | refresh 200, login 200, exactly 1 active token and it is the login token |
| E3 8 concurrent refreshes | same | 1 success, 7 AUTH-003, 1 active token |
| E4 8 concurrent logins | same | 8×200, exactly 1 active token |
| E5 each operation waits for the held user row | same | logout 200 after waiting 3337 ms while psql held `SELECT ... FOR UPDATE` |
| R-01〜R-07 re-run after the auth change | `DB=daipetto_r01 node scripts/e2e-api.mjs` | 12/12 pass, including 8 concurrent bookings → 1×201 + 7×409 RESERVATION-001 with no 5xx, approve-vs-reject and complete-vs-cancel 10/10 rounds consistent, suspended hospital 409 RESERVATION-009, reservation list 21/21 after deleting the pet |
| R-08 frontend queue | `cd frontend && node ../.../scripts/verify-r08.js` | both waiters rejected, auth cleared |

The old/new comparison is the direct answer to REVIEW-002's reproduction: with the lock, the bulk revoke in logout/login reports `UPDATE 1` instead of `UPDATE 0`, because it now runs after the refresh commits and therefore sees the successor row.

E1 and E5 are the "each real operation waits while the user row is held" proof that was pending: a psql session holds the row, the HTTP requests queue behind it (visible as multi-second waits), and the asserted end state holds afterwards.

## Limitations

- These are manual scripts, not part of `./gradlew test`. The automated suite still has no PostgreSQL concurrency coverage; adding it would mean introducing Testcontainers, which is a separate decision.
- The H2 test profile disables Flyway, so no automated test replays V8. V8 is exercised only by the scratch-database runs.
- E1/E2 force the ordering with an external lock holder rather than by pausing application code, so they prove the queueing and the resulting invariants, not a specific internal interleaving. The deterministic interleaving evidence is `r01-sql.sh` and REVIEW-003's own SQL runs.
- I did not drive the browser login form (I do not type passwords into browser fields). R-08 is covered by the harness; the backend refresh paths are covered above.
- V8 has still **not** been applied to the development database `daipetto`. It must be applied there before use, after resolving any pre-existing duplicate active reservations.
- R-05 concurrent cutoff, the `type`-claim rollout, the MapStruct `suspend` warning and the `JwtProvider` port observation remain documented, unchanged items.

## Response to previous review

| Item | Disposition | Evidence |
|---|---|---|
| R-01 source fix | Resolved; now also verified through HTTP | E1–E5, old/new SQL comparison |
| Scripts not found in the shared folder | Fixed | `scripts/` + `scripts/README.md` committed in this task folder |
| Full-stack HTTP concurrency unrun | Executed | table above, 6/6 and 12/12 |
| Reservation adapter constraint narrowing | Unchanged since round 3 | regression tests pass in the 161-test run |
| R-02〜R-08 | Keep REVIEW-002 dispositions | re-run of the 12 API checks after the auth change shows no regression |

## Receiver instructions

Verify the working tree against `HANDOFF-004-SHA256.txt` (identical to round 3). Re-running any script needs the `daipetto-postgres` container; each one manages its own scratch database. Source write ownership returns to no one with this round. Note that the user intends to commit and push this branch; the commit will contain exactly this snapshot plus these collaboration records.
