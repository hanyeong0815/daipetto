# Verification scripts for the user-management round

Manual checks, not part of `./gradlew test`. They need the `daipetto-postgres` container and a backend on
a fresh throwaway database. Nothing is written to `daipetto`; port 8080 is not used. Fixture credentials
exist only in the scratch database.

## `user-management-acceptance.mjs` (15 checks)

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_um_1021;"
cd spring-api && ./gradlew bootJar && cd ..
java -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_um_1021 --server.port=18081
BASE=http://localhost:18081 DB=daipetto_um_1021 \
  node .ai-collab/tasks/2026-10-21-user-management/scripts/user-management-acceptance.mjs
# optional regression on the same backend (own fixture users):
BASE=http://localhost:18081 DB=daipetto_um_1021 \
  node .ai-collab/tasks/2026-09-13-implementation-review/scripts/e2e-api.mjs
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_um_1021;"
```

Covers:

- ROLE-T007 403s.
- Search with no filters on PostgreSQL (null parameters), no password and `+09:00` in the response, case-insensitive keyword, role filter, paging.
- Unknown enum in query and JSON → 400.
- Self → USER-005; missing/deleted → USER-003.
- Suspension: the same access token goes 200 → 401, refresh tokens are revoked, refresh fails, login → AUTH-002.
- Idempotent suspend, then activate and log in.
- Role change visible to the same access token on the next request (403 → 200 → 403).

The race check uses a psql session that plays an in-flight refresh. It holds `SELECT ... FOR UPDATE` on the user row and inserts a successor token. The suspension request must block on that lock (checked in `pg_stat_activity`, throws otherwise), then revoke the successor after commit.

A second race check (ADMIN-T007, added for REVIEW-001 R-01) has psql play the suspension instead. psql holds the user row lock, sets SUSPENDED and revokes tokens. The login request has already read ACTIVE, so it must block on the session lock (checked in `pg_stat_activity`). After commit, the login must re-read the status and return 403 AUTH-002 with no active refresh token.
