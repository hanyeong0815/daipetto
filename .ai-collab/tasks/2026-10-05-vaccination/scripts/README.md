# Verification scripts for the Vaccination round

Manual checks, not part of `./gradlew test`. They need the `daipetto-postgres` container and a backend
started against a throwaway database. Nothing is written to `daipetto`, and the usual 8080 backend is
not used. Fixture credentials exist only in the scratch database.

## `vaccination-acceptance.mjs` — HTTP + PostgreSQL (13 checks)

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_vac_1021;"
cd spring-api && ./gradlew bootJar && cd ..
java -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_vac_1021 --server.port=18081
BASE=http://localhost:18081 DB=daipetto_vac_1021 \
  node .ai-collab/tasks/2026-10-05-vaccination/scripts/vaccination-acceptance.mjs
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_vac_1021;"
```

Run it once on a fresh database (it registers fixed fixture users). It covers VAC-T001..T005 (register and
list order, other-user 403, cancel/complete/reschedule transitions, 409 on disallowed transitions, soft
delete and 404 on a second delete), and three forced races. In each race a psql session holds the competing
change uncommitted, the HTTP request is sent, and the script waits until `pg_stat_activity` shows it blocked
on `update vaccinations` (it throws otherwise) before the holder commits:

- cancel behind a committed completion → 409 VACCINATION-003, row stays COMPLETED
- delete behind a committed delete → 404 VACCINATION-001
- cancel behind a committed delete → 409 VACCINATION-003, row stays deleted

## `TmpReminderPostgresCheck.java.txt` — REVIEW-001 R-03 on PostgreSQL

Temporary `@SpringBootTest` (local profile). Copy it to `spring-api/src/test/java/koh/portfolio/springapi/` as
`.java`, then run it against a scratch database and remove it afterwards:

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_vac_r03;"
cd spring-api && DB_NAME=daipetto_vac_r03 ./gradlew test --tests '*TmpReminderPostgresCheck'
```

A second JDBC connection holds an uncommitted reschedule (new date D2, `reminded_date` NULL). The reminder
run is started, and the test waits until `pg_stat_activity` shows its claim `update vaccinations` blocked
(fails otherwise). Then the holder commits. The run must send nothing, and the next run must send exactly
one reminder for D2 and set `reminded_date = D2`.

## `TmpVaccinationPostgresCheck.java.txt`

The temporary `@SpringBootTest` from HANDOFF-001 (V1..V11 on PostgreSQL, reminder extraction, dedupe,
conditional updates, FK). Kept as text; it is not on the test source path.
