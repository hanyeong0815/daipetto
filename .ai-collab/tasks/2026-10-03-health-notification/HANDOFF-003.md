# Handoff 003: 2026-10-03-health-notification

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-04, Asia/Tokyo
- Revision: revised before consumption, on the user's request, to add the JST timezone policy. No earlier version of this round was reviewed. The patch and manifest below replace the first version's.
- Task specification: none (same as HANDOFF-002)
- Recipient: Codex
- Requested action: re-review of R-04 and T-01, and review of the timezone change
- Consumed review: [REVIEW-002.md](REVIEW-002.md) (not edited)
- Source base SHA: f0faba909345bf8f109141490c82a4ef1e096303
- Source head SHA: same; all changes are uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Checkout/branch: `feat/spring/health-notification`, dirty
- Snapshot: `HANDOFF-003.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`, new files intent-added; 77 files). `HANDOFF-003-SHA256.txt` has 81 entries: those 77 plus the four implementer scripts in `scripts/` changed this round. HANDOFF-002 and its manifest are untouched; its manifest no longer matches the tree by design.

## Change summary

Files changed since HANDOFF-002:

- R-04: `UpdateHealthRecordService`, `HealthRecordRepository` (port), `HealthRecordJpaRepository`, `HealthRecordPersistenceAdapter`, `HealthRecordServiceTest`, `HealthRecordPersistenceAdapterTest`.
- Timezone: `SpringApiApplication`, `common/time/ServerTime`, new `frontend/src/utils/date.ts`, `frontend/src/pages/HealthRecordPage.tsx`, `frontend/src/pages/ReservationPage.tsx`.
- Docs: docs/06, docs/07, docs/10, docs/11, docs/12, AGENTS.md, TODO.md.
- Scripts: `smoke-health-notification.mjs`, `fix-acceptance.mjs`, `README.md`, new `r04-acceptance.mjs`.

| Finding | Change |
|---|---|
| R-04 | `UpdateHealthRecordService` reads the record with `findByIdForUpdate`: `@Lock(PESSIMISTIC_WRITE)` + `where id = :id and deletedAt is null`, emitted by Hibernate on PostgreSQL as `... for no key update`. Read, merge and the conditional `updateContent` now run under the row lock in one transaction, so another PATCH or soft delete on the same record is serialized. Omitted fields take the latest committed value. Same-field concurrent PATCHes remain last-committed-wins. The `deleted_at IS NULL` predicate stays on both the locked read and the UPDATE. Blank-text clearing and the empty-PATCH no-op are unchanged. |
| T-01 | Smoke expects the server-local date in Asia/Tokyo (`ServerTime.ZONE_ID`), sampled before and after the request, and accepts either value. |
| Timezone (user decision) | The service is Japan-facing, so the reference zone is JST (`Asia/Tokyo`). `ServerTime` changed from `Asia/Seoul`, which was unused, to `public static final ZONE_ID = Asia/Tokyo`. `SpringApiApplication.main` calls `TimeZone.setDefault(ZONE_ID)` before `SpringApplication.run`, so every existing `LocalDate/LocalDateTime.now()` and the log timestamps use JST whatever the host or container zone; the PostgreSQL JDBC session zone follows that default. The frontend computed "today" with `toISOString()` (UTC) in two places: the health-record form default and the reservation date `min`. Both now use `todayInJapan()`, which formats with `Intl.DateTimeFormat` in Asia/Tokyo through `formatToParts`. |

This production date change is a user policy decision, not a change made to satisfy T-01. The smoke assertion passes with or without it when the JVM runs in a UTC+9 zone. The difference shows only when the JVM is UTC; see the evidence. Korea and Japan are both UTC+9 with no DST, so existing `TIMESTAMP` data written under KST needs no migration.

Design choice for R-04: the reviewer offered per-field CASE/flags or a row lock. I chose the lock because it uses the existing pattern (`UserJpaRepository.findByIdForUpdate`). It keeps the clear-on-blank rule in the domain instead of in SQL. Also, the domain object built under the lock reflects the committed row; with CASE it would not.

Scope check: no other update path merges omitted fields from a read row. Hospital info (`name`/`address` required, `phoneNumber` replaced) and Pet (full replacement, disclosed in HANDOFF-002) write what the request carries.

## Acceptance evidence

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `./gradlew clean test`, `spring-api/`, after all changes | Java 17, H2 | exit 0; XML totals: 209 tests, 0 failures/errors/skips (was 208) |
| R-04 unit | `HealthRecordServiceTest` update tests stub `findByIdForUpdate`; `patchAndCapture` asserts `findById` is never called | Mockito | PASS |
| R-04 SQL predicate | `HealthRecordPersistenceAdapterTest.find_by_id_for_update_returns_only_active_row` | `@DataJpaTest`, H2 | PASS |
| Frontend | `npm run build` (`tsc -b && vite build`), `frontend/` | Node 24.17.0 | exit 0. `npm run lint` cannot run: ESLint is not installed or configured in the project (pre-existing). |
| Frontend date logic | Node evaluation of `todayInJapan()`'s body at 2026-10-03T15:58Z | Node 24.17.0 | `2026-10-04`; the replaced `toISOString().split('T')[0]` gave `2026-10-03` |

PostgreSQL runs used the reviewer's environment: boot jar, local profile, fresh `daipetto_review_1003`, port 18081, PostgreSQL 16.9. There were two runs, each on a fresh database:

| Run | JVM zone | Script order | Outcome |
|---|---|---|---|
| A, R-04 code before the timezone change | host default (KST) | smoke → review-notification-checks → review-002-lost-update → fix-acceptance → r04-acceptance → e2e-api | smoke 14/14 at 00:43 (UTC still the previous day); notifications 4/4; reviewer reproduction exit 1 at its interleaving guard (see below); fix-acceptance 10/10; r04 3/3; e2e 12/12 |
| B, final snapshot | started with `-Duser.timezone=UTC` deliberately | smoke → review-notification-checks → fix-acceptance → r04-acceptance → e2e-api | smoke 14/14 at 15:58Z: defaulted `recorded_date=2026-10-04` and `created_at=2026-10-04 00:58:34` (JST), and the log shows `+09:00`, proving `main` pins JST; notifications 4/4; fix-acceptance 10/10; r04 3/3; e2e 12/12 |

`review-002-lost-update.mjs` (unmodified) exits 1 at its interleaving guard ("Expected interleaving not reached"). The PATCH now waits on the locking SELECT, never on `update health_records`. After the guard's COMMIT the pending PATCH finished; final row `5.30/concurrent-memo`.

`r04-acceptance.mjs` uses the reviewer's method. It holds an uncommitted lock in psql, sends the real HTTP request(s), and waits until `pg_stat_activity` shows them `wait_event_type='Lock'` on `update health_records%` or `select % from health_records % for %` (throws otherwise). Then the holder commits. Results, identical in runs A and B:

```text
weight-only PATCH behind held memo UPDATE            200  5.30/cough/concurrent-memo/2026-05-20
empty PATCH behind held memo UPDATE                  200  4.70/cough/concurrent-memo/2026-05-20
memo-only + weight-only HTTP PATCHes, both queued
  behind a held SELECT ... FOR UPDATE                200/200  5.30/cough/http-memo/2026-05-20
```

The third case is the two-field concurrent PATCH requested in REVIEW-002, with both writers going through HTTP. It waits until both requests are blocked.

`fix-acceptance.mjs` change: its wait predicate only accepted `update <table>%`. The health-record PATCH now blocks earlier, on the locking SELECT, so R-02 would have hit the guard. The predicate now also accepts `select % from <table> % for %`. Assertions are unchanged. R-02 still returns 404 HEALTH-001 with the record deleted and `memo='original'`.

After each run the backend was stopped, `daipetto_review_1003` dropped, and the temporary `.claude/launch.json` entry reverted. `daipetto` and the 8080 instance were not touched.

## Limitations

- The PostgreSQL interleavings are manual scripts, not Gradle/CI. Mocks and H2 cannot show two sessions (docs/11 note under §6-4).
- The lock is held for the update transaction, which also does the owner lookup (one pet read). A PATCH waits behind any open transaction holding that row, with no `lock_timeout`. This matches the existing user-row lock used for sessions.
- The non-owner check happens after the lock is taken. A rejected request can briefly hold the row lock but cannot change the row.
- The JST pin is in `main`, so it covers bootRun, the jar, IntelliJ and the container. It does not cover the Gradle test JVM, which runs in the host zone; no current test depends on the zone.
- The two changed screens are behind login and were not opened in a browser, to avoid entering credentials there. They are covered by the type-checked build and the helper evaluation above.
- Reported, not changed: the docs/07 §2-7 example carries an offset (`+09:00`), but the API serializes `LocalDateTime` without one. Recorded in TODO.md for a user decision.
- Django has no settings yet. AGENTS.md §9 records that its `TIME_ZONE` must be `Asia/Tokyo` when it is built.
- Unchanged from HANDOFF-002: the Pet `updateProfile` column list must gain pet-fields columns at merge, and the docs/07 §5-4 Pet partial-update wording awaits a user decision.

## Documentation

- docs/07 §2-7: reference zone JST and where it is applied. §6-3: PATCH concurrency guarantee (locked read, latest committed value for omitted fields, same-field last-committed-wins).
- docs/06 §5: `TIMESTAMP` columns hold JST wall-clock time.
- docs/10 §3: Timezone row.
- docs/11 §6-4: HEALTH-T009, plus a note on which layer verifies T008/T009.
- docs/12 §3-12 addendum: a conditional UPDATE protects only its `WHERE` preconditions, not columns written from the read. §3-13: UTC-based "today" off by one day between 00:00 and 09:00 JST.
- AGENTS.md §3 rule 5 (locked read for merge-based partial updates), §8 (209), §9 (Timezone row), §10 (REVIEW-002 status). TODO.md: REVIEW-002 entries and the pending offset decision. `scripts/README.md`.

## Response to previous review

| ID | Disposition | Evidence | Remaining action |
|---|---|---|---|
| R-04 | Fixed: locked read serializes read/merge/write | unit + H2 predicate test; r04-acceptance 3/3 in runs A and B; reviewer reproduction no longer reaches the defect state | reviewer re-check |
| T-01 | Fixed in the oracle (Asia/Tokyo, either side of midnight) | smoke 14/14 in runs A (00:43 JST) and B (UTC JVM) | none |
| R-01/R-02/R-03/D-01 | Resolved in REVIEW-002; retained | fix-acceptance 10/10 in both runs | none |

## Receiver instructions

Verify against `HANDOFF-003-SHA256.txt` from the repository root (`sha256sum -c`), or apply `HANDOFF-003.patch` onto f0faba9. Reproduce as in HANDOFF-002 and `scripts/README.md`: fresh `daipetto_review_1003`, jar on :18081, `BASE`/`DB` set, then smoke → `review-*.mjs` → `fix-acceptance.mjs` → `r04-acceptance.mjs`. Add `-Duser.timezone=UTC` to the `java` command to check the JST pin. `review-002-lost-update.mjs` is expected to stop at its interleaving guard. To watch its original scenario reach completion, accept the locking SELECT in its wait predicate; the final state is then `5.30/concurrent-memo`. Source write ownership returns to no one with this round. No commit, merge or deployment is implied.
