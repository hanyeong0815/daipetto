# Verification scripts for this task

Manual checks used in rounds 002–004. They are not part of `./gradlew test`; they need the
`daipetto-postgres` container (PostgreSQL 16.9) and, for the HTTP ones, a backend started against a
throwaway database. Every script creates and drops its own scratch database and never touches `daipetto`.

Run all commands from the repository root unless noted.

## `r01-sql.sh` — R-01 mechanism, old vs new statement order

```bash
bash .ai-collab/tasks/2026-09-13-implementation-review/scripts/r01-sql.sh
```

Applies `V1`–`V8` to a scratch database, then replays REVIEW-002's interleaving (refresh pauses
between consuming the old token and inserting the successor, while logout/login runs) twice: once
without the user-row lock and once with it. Prints the affected-row counts and the surviving active
tokens for each case, then drops the database.

## Isolated backend for the HTTP scripts

```bash
cd spring-api && ./gradlew bootJar
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_r01;"
java --enable-preview -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_r01 --server.port=18080
```

Flyway applies `V1`–`V8` on startup. Use `--DB_NAME=daipetto_e2e` for `e2e-api.mjs`. Drop the
database when finished. Port 18080 keeps the normal 8080 instance untouched.

## `r01-http.mjs` — R-01 through the running Spring stack

```bash
node .ai-collab/tasks/2026-09-13-implementation-review/scripts/r01-http.mjs
```

Holds `SELECT ... FOR UPDATE` on the user row from a psql session so the HTTP operations queue in a
known order, then asserts: E1 refresh-then-logout leaves no active session and the racing successor
is already revoked, E2 refresh-then-login leaves exactly the login token, E3 eight concurrent
refreshes leave one winner, E4 eight concurrent logins serialize to one active token, E5 logout
visibly waits for the held row. Exit code is non-zero if any check fails.

## `e2e-api.mjs` — R-01〜R-07 through the running Spring stack

```bash
node .ai-collab/tasks/2026-09-13-implementation-review/scripts/e2e-api.mjs
```

Needs the backend on `--DB_NAME=daipetto_e2e`. Twelve checks covering same-second login tokens,
concurrent refresh, refresh-as-Bearer, concurrent booking of one schedule, approve/reject and
complete/cancel races (10 rounds each), suspended-hospital booking and reservation history after a
pet is deleted. Reset state between runs with
`TRUNCATE users, refresh_tokens, pets, hospitals, hospital_business_hours, hospital_schedules, reservations RESTART IDENTITY CASCADE;`.

## `verify-r08.js` — frontend refresh queue (R-08)

```bash
cd frontend && node ../.ai-collab/tasks/2026-09-13-implementation-review/scripts/verify-r08.js
```

Transpiles the real `src/api/client.ts` with the repo's TypeScript, stubs axios and the auth store,
fires two 401s and fails the refresh. Passes only when every waiter settles. Pass a file path as the
first argument to compare another revision, e.g.
`git show HEAD:frontend/src/api/client.ts > /tmp/before.ts` then
`node ../.ai-collab/.../verify-r08.js /tmp/before.ts` reproduces the original defect.

All fixture credentials in these scripts exist only inside the scratch databases.
