# Handoff 002 supplement: end-to-end evidence

- Author / implementer: Claude Code
- Timestamp: 2026-09-15, Asia/Seoul
- Applies to: the HANDOFF-002 snapshot, unchanged. Before and after this run, `sha256sum -c HANDOFF-002-SHA256.txt` matched all 39 files. No source was edited.
- Why a separate file: HANDOFF-002 was already published as ready, so it is left as-is. This file only adds evidence for the same snapshot.

## Environment

- Backend: `bootJar` built from the snapshot, run with `--spring.profiles.active=local --DB_NAME=daipetto_e2e --server.port=18080` (JDK 17.0.19)
- Database: throwaway `daipetto_e2e` inside the existing `daipetto-postgres` container (PostgreSQL 16.9). Flyway applied V1〜V8 on startup and `ddl-auto: validate` passed. The database was dropped afterwards; the dev database `daipetto` was not touched.
- Frontend: Vite dev server on 5173 with `VITE_API_BASE_URL_WEB=http://localhost:18080`
- The user's own IntelliJ instance (port 8080, dev DB) was left running and untouched. It started on 2026-09-13 20:43, which is before the fixes, so it runs pre-fix code.
- Fixture accounts existed only in the throwaway database.

## Results: 12/12 API checks (Node `fetch`, concurrent requests via `Promise.all`)

| Finding | Check | Observed |
|---|---|---|
| R-02 | two back-to-back logins sharing `iat` | `iat=1789480454`, distinct tokens, 2 rows persisted, no 500 |
| R-01 | 8 concurrent `/auth/refresh` with one token | `{"200":1,"401 AUTH-003":7}`, 1 active token for the user |
| R-01 | replay the consumed token | 401 AUTH-003 |
| R-07 | refresh token as `Authorization: Bearer` on `/users/me` | 401 AUTH-001 (no filter exception); the access token still returns 200 |
| R-03 | 8 concurrent bookings of one schedule | `{"201":1,"409 RESERVATION-001":7}`, 0 5xx, 1 active row |
| R-04 | approve vs reject submitted concurrently, 10 rounds | 10/10 rounds: exactly one 200, one 409 RESERVATION-008, stored status = winner |
| R-04 | complete vs cancel submitted concurrently, 10 rounds | 10/10 rounds consistent, same criteria |
| R-05 | book after `PATCH .../suspend` | 409 RESERVATION-009 |
| R-06 | delete the pet, then list reservations | 200, 21/21 reservations rendered with the pet name |

The R-03 run reached the database constraint, not only the application pre-check. The backend log records 7 `duplicate key value violates unique constraint "uq_reservations_active_schedule"` errors on 7 different Tomcat threads (`exec-1,2,5,6,8,9,10`) at `22:53:36.614`. So all 7 losers had passed `existsActiveByScheduleId` concurrently, which is the exact race in R-03. Every one was translated to 409 RESERVATION-001. The log has 0 `SERVER-001` / 500 entries.

## Browser check: R-08 in the real app

In the page on 5173 I set bogus access and refresh tokens (not credentials) and issued two concurrent `apiClient` calls.

- Network: `GET /pets → 401`, `GET /users/me → 401`, then exactly **one** `POST /auth/refresh → 401`. The second request queued rather than starting its own refresh.
- Result: `{ first: "rejected(401)", queued: "rejected(401)", refreshTokenAfter: null, isAuthenticatedAfter: false }`. Neither promise stayed pending.
- Console: only the three expected `Failed to load resource: 401` entries. No uncaught errors.

## Not covered

- I did not drive the login form's golden path in the browser, because I do not type passwords into browser fields. The refresh success branch of `client.ts` is covered by the isolated harness in HANDOFF-002, and the backend refresh success path is covered by the R-01 checks above.
- For R-04, concurrent submission does not guarantee that the two transactions interleave on every round. The deterministic interleaving proof is the psql `pg_sleep` transcript in HANDOFF-002. These rounds show that concurrent HTTP submission never produces two successes or an overwritten terminal state.
- These are manual runs. The suite still has no automated PostgreSQL concurrency test. So the related lines in AGENTS.md §8/§10 and TODO.md are still accurate for automated coverage. They are frozen with the snapshot and should be refreshed once this review round closes.
