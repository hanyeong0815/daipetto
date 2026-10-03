# Verification script for the HealthRecord / Notification round

Manual smoke check, not part of `./gradlew test`. It needs the `daipetto-postgres` container and a
backend started against a throwaway database. It creates nothing in `daipetto`.

```bash
# 1. scratch database
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_hn;"

# 2. backend from this snapshot on a port that does not disturb the usual 8080 instance
cd spring-api && ./gradlew bootJar
java --enable-preview -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_hn --server.port=18080

# 3. checks (14)
node .ai-collab/tasks/2026-10-03-health-notification/scripts/smoke-health-notification.mjs

# 4. cleanup
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_hn;"
```

Covers HEALTH-T001〜T005 (create, owner rejection, negative weight, list order, logical delete),
the `recordedDate` default (compared with the server-local Asia/Tokyo date, either side of midnight) and
update behaviour, NOTI-T001〜T005 (one notification per reservation
approve/reject/complete, user scoping, read marking with `read_at`), read idempotency, the
NOTIFICATION-001/002 paths, and that cancelling a reservation creates no notification.

Reset between runs with
`TRUNCATE users, refresh_tokens, pets, hospitals, hospital_business_hours, hospital_schedules, reservations, health_records, notifications RESTART IDENTITY CASCADE;`.

## `fix-acceptance.mjs` — REVIEW-001 fixes (10 checks)

Run right after the smoke script, on the same backend and database:

```bash
BASE=http://localhost:18081 DB=daipetto_review_1003 \
  node .ai-collab/tasks/2026-10-03-health-notification/scripts/fix-acceptance.mjs
```

Each race is forced deterministically: a psql session holds the conflicting change uncommitted, the
HTTP request is sent, the script waits until `pg_stat_activity` shows that request blocked on its own
`update <table>` (it throws otherwise), then the holder commits. Asserts the fixed behaviour for
R-01 (omitted/empty/blank PATCH), R-02 (PATCH behind a soft delete), R-03 and its mirror (info update
vs suspension), and the same pattern in Pet and HospitalBusinessHours updates.

Since the REVIEW-002 R-04 fix, a health record PATCH blocks on its locking
`select ... from health_records ... for no key update` instead of its UPDATE; the wait predicate accepts both.

## `r04-acceptance.mjs` — REVIEW-002 R-04 (3 checks)

Same method and environment variables, run after `fix-acceptance.mjs`. Asserts the fixed outcome of the
reviewer's reproduction (weight-only PATCH behind a held memo update keeps `concurrent-memo`), that an
empty PATCH does not undo a concurrent update, and that two HTTP PATCHes to different fields, both queued
behind a held row lock, both survive.

## Order

smoke → `review-*.mjs` → `fix-acceptance.mjs` → `r04-acceptance.mjs`. The reviewer scripts are pinned to
`daipetto_review_1003` / port 18081 and expect the database state right after the smoke script;
`fix-acceptance.mjs` adds a second pet to `hn-user`. `review-002-lost-update.mjs` waits for the PATCH to
block on `update health_records`; after the fix it blocks earlier, on the locking SELECT, so that script
now stops at its interleaving guard.

Fixture credentials in the scripts exist only inside the scratch database.
