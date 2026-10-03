# Review 001: HealthRecord / Notification and overall regression review

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-03, Asia/Seoul
- Requested directly by the user; no new-domain HANDOFF or TASK was present. Implementer: Claude Code. No implementer-owned status record was changed.
- Branch: `feat/spring/health-notification`
- Source base / HEAD: `f0faba909345bf8f109141490c82a4ef1e096303` plus dirty source.
- Snapshot: `REVIEW-001.patch` captures tracked changes; `REVIEW-001-untracked/` captures 40 new source/test/migration files; `REVIEW-001-SHA256.json` identifies 291 application/frontend/doc/policy files. `REVIEW-001-ARTIFACT-SHA256.json` identifies captured artifacts. Unrelated `spring-api/.claude/` was excluded. No application source edits were made by the reviewer.
- Verdict: **CHANGES_REQUESTED**
- Scope: new HealthRecord CRUD and Notification APIs, reservation hooks, authentication/authorization boundaries, persistence/concurrency, existing auth/reservation/frontend regressions, and relevant API/ERD/state/test documentation. No practice gap was excluded for these two new APIs: TODO explicitly says this round has none.
- IDs below are local to this task, not the R-01..R-08 IDs of the September review.

## Findings

### R-01 [P1] HealthRecord partial updates silently erase omitted values

- Location: `spring-api/src/main/java/koh/portfolio/springapi/application/healthrecord/service/UpdateHealthRecordService.java:33-37`.
- Create a record with weight `4.70`, symptom `cough`, memo `original`, then PATCH only `{"memo":"memo-only"}`. Actual HTTP response: 200; stored fields: `NULL/NULL/memo-only`.
- The API design, docs/07 line 571, promises a partial update. Only recordedDate currently preserves its previous value when omitted. The same loss occurs with other single-field changes and an empty body object.
- Impact: ordinary client requests silently destroy recorded weight/symptom/memo data.
- Fix: define omitted versus explicit-null behavior and apply updates only to supplied fields; preserve existing values on omission. Add single-field and empty PATCH regression checks; if explicit clearing is supported, cover it separately.
- Evidence: reviewer-executed HTTP + PostgreSQL reproduction in `scripts/review-health-repro.mjs` (exit 0, assertions intentionally confirm the defective behavior).
- Disposition: open; high confidence.

### R-02 [P2] HealthRecord PATCH can restore a concurrently deleted record

- Location: `spring-api/src/main/java/koh/portfolio/springapi/application/healthrecord/service/UpdateHealthRecordService.java:40`; persistence path `infrastructure/persistence/healthrecord/HealthRecordPersistenceAdapter.java:19-20` under the same Java root.
- PATCH reads the active row, a separate deletion commits, then PATCH flushes the previously read entity. Full-entity merge writes the old `deletedAt=null`; no version or conditional active-row update protects it.
- Deterministic reproduction: a SQL transaction performs the same soft-delete mutation as the DELETE adapter and holds its row lock. Real HTTP PATCH reads the prior MVCC version, reaches its UPDATE and waits (verified in pg_stat_activity). Commit the delete, then PATCH completes 200. Stored `deleted_at IS NULL` becomes true and the record appears in the real GET list again.
- This is a SQL-delete/HTTP-PATCH integration reproduction, not a claim that two HTTP endpoints were paused internally.
- Fix: update only intended mutable columns with `WHERE deleted_at IS NULL` and check the affected count, or use coordinated locking/version checks for all writers. A transaction annotation alone does not resolve this interleaving. Cover deletion-before-patch-write and preserve deletion as the final state.
- Evidence: `scripts/review-health-repro.mjs`: `200 true/racing visible: true`.
- Disposition: open; high confidence.

### R-03 [P2] Existing hospital information updates can undo suspension

- Location: `spring-api/src/main/java/koh/portfolio/springapi/application/hospital/service/UpdateHospitalService.java:27`.
- **Pre-existing defect found by this overall review; not introduced by Notification/HealthRecord.** An information PATCH reads ACTIVE, a suspension commits, and the stale full-entity save writes ACTIVE back. Updating contact information must not reverse the separately controlled suspension state.
- Deterministic reviewer reproduction: hold a SQL `status='SUSPENDED'` update uncommitted; issue the real information PATCH; observe it waiting on its UPDATE; commit suspension. PATCH returns 200 and stored status is ACTIVE.
- Impact: the suspended hospital becomes eligible for new reservations again. Information editing is allowed for hospital admins, while the suspend endpoint is restricted to system admins; preserving this separation requires preventing unrelated info updates from overwriting status.
- Fix: use column-specific updates for information versus status, or consistent concurrency control across both writers. Check the equivalent full-entity save pattern in other mutable domains while fixing it. Add an information-update versus suspension regression.
- Evidence: `scripts/review-health-repro.mjs`: `R-03 ... 200 ACTIVE`. Suspension was a SQL mutation with the same relevant state change, not a second HTTP call in this reproduction.
- Disposition: open; high confidence.

### D-01 [P3] Implementation inventory still calls the new domains unimplemented

- `docs/06_ERD 363d097bd4f780699f2cd5859607c3c8.md:193,195` marks health_records and notifications `未作成`, despite V9/V10 and the new APIs.
- `AGENTS.md` section 6 still says the Notification domain is not built; section 10 says it is complete.
- Correct these inventory statements during the fix handoff. API/ERD content otherwise describes the main new fields and behavior. Older TODO statements saying reservation backend is unimplemented also remain stale.
- Disposition: open; nonblocking documentation correction.

## Architecture / security assessment

- New domains use Controller -> UseCase -> Service -> Domain/Port -> Adapter/MapStruct/JPA, with no new domain-to-JPA dependency found. The important exception to the preferred explicit-update policy is the unsafe HealthRecord save in R-02.
- HealthRecord create/list/update/delete ownership is derived through Pet.userId. Notification lists filter by the authenticated user and reads check the recipient. The reviewer exercised the real security chain, including anonymous and cross-user requests.
- Reservation approve/reject/complete notify only after a successful conditional state transition and participate in the same transaction. Actual notification INSERT failure rolled back the reservation transition. An approve/reject race produced one matching notification.
- Repeated reads preserve the first read_at, verified with eight concurrent repeat requests.
- Existing hospital affiliation authorization remains intentionally absent until hospital_admins exists; it is a known limitation, not a newly introduced finding. New frontend HealthRecord/Notification wiring, Vaccination/Scheduler/Django are outside the implemented scope.

## Reviewer-executed verification

All commands used the snapshot above on Windows, Java 17.0.19, PostgreSQL 16.9. Script paths are relative to the repository root unless noted. Every command below exited 0.

| Check | Command / directory | Actual outcome |
|---|---|---|
| Entire Spring suite | `./gradlew.bat clean test`, `spring-api/` | 192 tests, 0 failures/errors/skips (XML totals); existing HospitalMapper `suspend` warning remains |
| Frontend production build | `npm run build`, `frontend/` | TypeScript + Vite pass |
| Runtime artifact | `./gradlew.bat bootJar`, `spring-api/` | Built successfully |
| Migrations and entity validation | Start bootJar with local profile, `--DB_NAME=daipetto_review_1003 --server.port=18081` | V1-V10 applied to empty scratch DB; Hibernate validate and startup passed |
| New-domain smoke | `node .ai-collab/tasks/2026-10-03-health-notification/scripts/smoke-health-notification.mjs` | 14/14 pass |
| Existing auth/reservation regression | `node .ai-collab/tasks/2026-09-13-implementation-review/scripts/e2e-api.mjs` | 12/12 pass: refresh replay/concurrency, token separation, duplicate bookings, competing state transitions, suspension guard, deleted-pet history |
| Frontend refresh failure queue | `node ../.ai-collab/tasks/2026-09-13-implementation-review/scripts/verify-r08.js`, `frontend/` | Both requests rejected, auth cleared, no hanging waiter |
| Additional notification/security integration | `node .ai-collab/tasks/2026-10-03-health-notification/scripts/review-notification-checks.mjs` | Anonymous/cross-user access blocked; repeat reads preserve timestamp; notification failure rolls back reservation; competing transitions create one notification |
| Defect reproductions | `node .ai-collab/tasks/2026-10-03-health-notification/scripts/review-health-repro.mjs` | Confirmed R-01/R-02/R-03; these are defect-reproducing assertions, not passing acceptance tests |

Database scripts used `DB=daipetto_review_1003`, `BASE=http://localhost:18081`. Only this throwaway database was mutated. The ordinary development database was not used. The dedicated backend and scratch DB were removed after verification.

The existing 192-test suite and 14-check smoke do not cover omitted-field preservation or stale full-entity writes, which explains why both passed despite R-01/R-02. H2 tests still do not replay PostgreSQL Flyway migrations; the reviewer explicitly exercised them in the scratch DB instead.

## Reproduction instructions

1. Build with `./gradlew.bat bootJar` in `spring-api/`.
2. Create the dedicated scratch database with `docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_review_1003;"`.
3. From `spring-api/`, start `java --enable-preview -jar build/libs/spring-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --DB_NAME=daipetto_review_1003 --server.port=18081`. Existing local configuration supplies credentials; do not publish them.
4. From the repository root in PowerShell, set `$env:DB='daipetto_review_1003'` and `$env:BASE='http://localhost:18081'`. Run the original smoke first to create fixtures, then the two reviewer scripts. The reviewer scripts deliberately reject other targets.
5. Stop that dedicated backend and drop only `daipetto_review_1003`. Do not run these fixture scripts against the development database.

## Prior handoff and remaining limits

- Read September-task HANDOFF-004. Its E1-E5 auth concurrency evidence is implementer-reported in this round; this review personally re-ran the 12-check API regression and frontend queue harness, not the separate r01-http E1-E5 script. The historical task's status was not changed.
- No browser interaction, mobile runtime, load test, or dependency vulnerability audit was performed. Passing selected regressions is not exhaustive proof of the whole application.
- Reviewer-owned scripts are manual checks, not wired into Gradle/CI. Extend durable regression coverage for the accepted fixes rather than relying solely on this report.
- Next actor: Claude Code should fix R-01/R-02/R-03, synchronize D-01, and publish a new handoff with source identity, regression results and an explicit omitted/null PATCH policy. Do not overwrite this consumed review round. A file handoff does not automatically notify Claude.
