# Verification scripts for the hospital-admins round

Manual checks, not part of `./gradlew test`. They need the `daipetto-postgres` container and a backend on
a fresh throwaway database. Nothing is written to `daipetto`; port 8080 is not used. Fixture credentials
exist only in the scratch database.

## `hospital-admins-acceptance.mjs` (13 checks)

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_ha_1022;"
cd spring-api && ./gradlew bootJar && cd ..
java -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_ha_1022 --server.port=18081
BASE=http://localhost:18081 DB=daipetto_ha_1022 \
  node .ai-collab/tasks/2026-10-22-hospital-admins/scripts/hospital-admins-acceptance.mjs
# optional regression on the same backend (own fixture users):
BASE=http://localhost:18081 DB=daipetto_ha_1022 \
  node .ai-collab/tasks/2026-09-13-implementation-review/scripts/e2e-api.mjs
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_ha_1022;"
```

Covers:

- HA-01..HA-05: unlinked vs linked hospital admin on hospital update, business hours, schedules and slot blocking (403 HOSPITAL-005, no change); SYSTEM_ADMIN on any hospital.
- HA-02/HA-03/HA-10: link (201, 409 HOSPITAL-007, 400 HOSPITAL-006, 404 HOSPITAL-001, 400 without `userId`, 403 for a hospital admin), admin list, unlink (404 HOSPITAL-008 the second time; access lost on the next request).
- HA-06/HA-07: admin reservation list on PostgreSQL with null filters, nickname/pet/memo, `+09:00`; status/date filters; other hospital 403; missing `hospitalId`, bad status, bad date → 400; ROLE_USER 403.
- HA-08: approve by an unlinked hospital admin is 403 and the status stays REQUESTED; the linked admin approves and completes.
- HA-09: `/mine` keeps SUSPENDED hospitals.
- HA-11: demotion deletes every link; promotion does not restore them.
- HA-12: 5 concurrent identical link requests → one 201, four 409, one row.
- HA-13 race: psql plays a demotion (UPDATE role + DELETE links, uncommitted). The link request must block on the user row lock (checked in `pg_stat_activity`, throws otherwise), then fail with 400 HOSPITAL-006 and leave no link.
