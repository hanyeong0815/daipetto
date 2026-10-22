# Verification scripts for the schedule-generation round

Manual checks, not part of `./gradlew test`. They need the `daipetto-postgres` container and a backend on
a fresh throwaway database. Nothing is written to `daipetto`; port 8080 is not used. Fixture credentials
exist only in the scratch database.

## `schedule-generation-acceptance.mjs` (9 checks)

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_gen_1022;"
cd spring-api && ./gradlew bootJar && cd ..
java -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_gen_1022 --server.port=18081
BASE=http://localhost:18081 DB=daipetto_gen_1022 \
  node .ai-collab/tasks/2026-10-22-schedule-generation/scripts/schedule-generation-acceptance.mjs
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_gen_1022;"
```

Months are computed relative to today's JST date, and expected slot counts are derived from the calendar
(Monday 09:00–12:00 with a 10:00–10:30 break in 30-minute slots = 6 slots incl. 1 BLOCKED; Wednesday
14:00–15:00 in a 60-minute slot = 1 slot).

- GEN-01: invalid business hours (reversed, 0 min, break outside, half break, invalid update) → 400 HOSPITAL-009.
- GEN-02/03: generate next month (exact slots/statuses on the first Monday, counts match the calendar); re-run creates nothing.
- GEN-04: a day holding a manually BLOCKED slot is skipped and left unchanged.
- GEN-05: past month and +13 → HOSPITAL-010; +12 OK; missing/bad month → 400; this month only from today.
- GEN-06: unlinked hospital admin → 403 HOSPITAL-005; ROLE_USER → 403; SYSTEM_ADMIN OK.
- GEN-07: a generated AVAILABLE slot can be booked; the generated break slot rejects booking (RESERVATION-003).
- GEN-08: 4 concurrent generations of one month → slots created once, no duplicate (date, start).
- GEN-09 race: psql holds the hospital row lock and inserts a slot on the first Monday. The HTTP generation must block on the lock (checked in `pg_stat_activity`, throws otherwise), then skip that day.

The monthly cron is not triggered here; it is unit-tested and calls the same per-hospital generator.
