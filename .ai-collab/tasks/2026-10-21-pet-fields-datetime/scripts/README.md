# Verification scripts for the pet-fields / datetime round

Manual checks, not part of `./gradlew test`. They need the `daipetto-postgres` container and a backend
started against a fresh throwaway database. Nothing is written to `daipetto`; port 8080 is not used.
Fixture credentials exist only in the scratch database.

## `pet-datetime-acceptance.mjs` (17 checks)

```bash
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_pet_1021;"
cd spring-api && ./gradlew bootJar && cd ..
java -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_pet_1021 --server.port=18081
BASE=http://localhost:18081 DB=daipetto_pet_1021 \
  node .ai-collab/tasks/2026-10-21-pet-fields-datetime/scripts/pet-datetime-acceptance.mjs
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_pet_1021;"
```

Checks: Flyway history is exactly V1..V14; create without gender (NULL stored); create with the new fields;
five validation cases return 400 VALIDATION-001; detail returns the new fields; `createdAt` matches
`^...T..:..:..+09:00$` and equals the stored JST value; `birthDate` has no offset; list carries breed;
PATCH is a full replacement (omitted fields become NULL/false); PATCH writes the new columns; invalid PATCH
leaves the row unchanged; a notification stored as `23:59:58.765` is returned as `2026-10-21T23:59:58+09:00`.

## Upgrade path (manual)

Migrate a fresh DB with `--spring.flyway.target=12 --spring.main.web-application-type=none`. The context then
fails schema validation, which is expected because the entity already has the V13 columns. Insert a user and a pet with gender via
psql, then start again without the target: V13 and V14 apply, the pet keeps its values with
`breed=NULL, neutered=false, microchip_number=NULL`, and `pets.gender` becomes nullable.
