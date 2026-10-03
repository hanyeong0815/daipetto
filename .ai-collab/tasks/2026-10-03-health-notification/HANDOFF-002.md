# Handoff 002: 2026-10-03-health-notification

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-03, Asia/Seoul
- Task specification: none — REVIEW-001 was requested directly by the user without a TASK.md; this record does not create one
- Recipient: Codex
- Requested action: re-review of R-01, R-02, R-03 and D-01
- Consumed review: [REVIEW-001.md](REVIEW-001.md) (not edited)
- Source base SHA: f0faba909345bf8f109141490c82a4ef1e096303
- Source head SHA: same — all changes are uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Checkout/branch: `feat/spring/health-notification`, dirty
- Snapshot: `HANDOFF-002.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api`, new files intent-added) and `HANDOFF-002-SHA256.txt` (71 entries)

Round 001 is unused because REVIEW-001 reviewed the implementation without a preceding handoff, the same convention as the September task.

## Root cause

R-01, R-02 and R-03 share one cause: update paths loaded an entity, changed it in the domain, and persisted it with `save()`. A full save rewrites every column with the values read earlier, so columns the operation never meant to change (`status`, `deleted_at`, omitted PATCH fields) are overwritten. The fix is the same everywhere: write only the columns the operation owns with a conditional `@Modifying` UPDATE, put the precondition in the `WHERE`, and treat 0 affected rows as not found.

## Change summary

| Finding | Change |
|---|---|
| R-01 | `HealthRecord.update` replaced by `HealthRecord.patch`: a null (omitted) field keeps its current value; a blank `symptom`/`memo` clears it to NULL; `weight` cannot be cleared by PATCH. An empty body is a no-op. |
| R-02 | `HealthRecordRepository.updateContent` writes weight/symptom/memo/recordedDate/updatedAt `WHERE id=? AND deleted_at IS NULL`. `UpdateHealthRecordService` maps 0 rows to HEALTH-001. `save()` is no longer used for updates. |
| R-03 | `HospitalRepository.updateInfo` (name/address/phoneNumber/updatedAt) and `updateStatus` (status/updatedAt) are separate UPDATEs, both `WHERE id=? AND deleted_at IS NULL`. Info editing no longer touches status, and suspension no longer rewrites info. Both services gained the missing `@Transactional`. |
| Same pattern, Pet | `PetRepository.updateProfile` `WHERE id=? AND deleted_at IS NULL`; 0 rows → PET-001. Previously a pet deleted after the PATCH read was restored. |
| Same pattern, HospitalBusinessHours | `updateHours` `WHERE id=? AND hospital_id=?`; 0 rows → HOSPITAL-003. Previously the stale merge could recreate a row deleted after the read. The hospital ownership check is now also atomic. |
| Not changed | HospitalSchedule block/unblock: a single-column status toggle with no other writer of its columns and no deletion. Last writer wins is the intended semantics. |
| D-01 | ERD inventory shows V9/V10; AGENTS.md §6 no longer says Notification is unbuilt; the two TODO lines claiming the reservation backend is unimplemented are corrected. |

Policy documented in `docs/07_API_Design` §6-3 (PATCH semantics) and `AGENTS.md` §3 rule 5 (`save()` only for inserts).

### Accepted limitation

Two concurrent PATCHes by the owner to the same health record are last-writer-wins at record level, because `patch` merges in memory from the row it read. Health records have a single owner, so this needs the same user to edit the same record twice at once. Deletion is never undone. Column-level `COALESCE` updates would remove this but complicate the explicit-clear rule; not done.

### Found while syncing docs (my earlier miss)

`docs/11` HOSP-T002 still expected RESERVATION-002 for a suspended hospital, although the September R-05 fix returns RESERVATION-009 (already documented in `docs/07` §9-3 and RSV-T007). Corrected.

### Reported, not changed

`docs/07` §5-4 calls pet update a partial update, but the implementation replaces `birthDate`/`weight` with NULL when omitted (name/petType/gender are required). The frontend always sends the full form, so there is no observed harm. Left for the user to decide; listed in TODO.md.

## Acceptance evidence

| Criterion | Command / directory | Outcome |
|---|---|---|
| Whole suite | `./gradlew clean test`, `spring-api/` | exit 0 — 208 tests, 0 failures/errors/skips (was 192) |
| R-01 unit | `HealthRecordServiceTest` (memo-only, empty body, blank clears, full update) | PASS |
| R-02 unit + SQL | `HealthRecordServiceTest.update_health_record_fail_when_deleted_after_read`; `HealthRecordPersistenceAdapterTest.update_content_does_not_resurrect_deleted_row` (`@DataJpaTest`, H2) | PASS — false returned, `deleted_at` unchanged |
| R-03 unit + SQL | `HospitalServiceTest`; `HospitalPersistenceAdapterTest.update_info_keeps_status` / `update_status_keeps_info` | PASS |
| Equivalents SQL | `PetPersistenceAdapterTest`, `HospitalBusinessHoursAdapterTest` (deleted row not recreated, other hospital rejected) | PASS |
| Interleaved PostgreSQL acceptance | `scripts/fix-acceptance.mjs` against bootJar on :18081, DB `daipetto_review_1003` | 10/10 — see below |
| Reviewer's defect reproduction, unmodified | `scripts/review-health-repro.mjs` | stops at its R-01 assertion: actual `4.70/cough/memo-only`, the defect's expected `NULL/NULL/memo-only` |
| Reviewer's notification checks, unmodified | `scripts/review-notification-checks.mjs` after a fresh smoke | 4/4 PASS |
| Smoke | `scripts/smoke-health-notification.mjs` | 14/14 PASS |
| Auth/reservation regression | `../2026-09-13-implementation-review/scripts/e2e-api.mjs` | 12/12 PASS |

`fix-acceptance.mjs` uses the reviewer's method. A psql session holds the conflicting mutation uncommitted. The real HTTP request is issued, and the script waits until `pg_stat_activity` shows it blocked on its own `update <table>`; if it never blocks, the script throws. Then the holder commits. Results:

```text
R-01 memo-only PATCH            200  4.70/cough/memo-only/2026-05-20
R-01 empty PATCH                200  unchanged
R-01 weight-only PATCH          200  5.30/cough/memo-only/2026-05-20
R-01 blank symptom              200  5.30/NULL/memo-only/2026-05-20
R-02 PATCH behind soft delete   404 HEALTH-001, deleted_at set, memo still 'original', not listed
R-03 info PATCH behind suspend  200, status SUSPENDED, new name applied
R-03 impact                     booking that hospital → 409 RESERVATION-009
R-03 mirror                     suspend behind info update → SUSPENDED, 'Held name' kept
Pet PATCH behind soft delete    404 PET-001, pet stays deleted with old name
Hours PATCH behind delete       404 HOSPITAL-003, 0 rows for that hospital
```

Run note: the first attempt at `review-notification-checks.mjs` failed with `pet_id=NaN`. The cause was my acceptance script, which adds a second pet to `hn-user`, while the reviewer script expects exactly one pet. After resetting the database and following the reviewer README order (smoke, then reviewer scripts), it passed. This was a fixture ordering conflict, not an application failure.

All verification used a throwaway database that was dropped afterwards; `daipetto` and the user's 8080 instance were not touched.

## Limitations

- Manual scripts are not wired into Gradle/CI. The durable coverage is the unit tests plus the `@DataJpaTest` predicate tests on H2. H2 cannot reproduce two concurrent sessions, so only the PostgreSQL runs exercise the interleavings.
- The Pet `updateProfile` query lists the current profile columns. The unmerged `feat/spring/pet-fields` branch adds breed/neutered/microchip_number; they must be added to this query at merge or they will silently stop updating.
- No browser E2E for the new screens; those screens are not wired to the APIs yet.

## Response to previous review

| ID | Disposition | Evidence |
|---|---|---|
| R-01 | Fixed; policy defined in docs/07 §6-3 | unit tests, acceptance rows 1–4, reviewer script no longer reproduces |
| R-02 | Fixed | unit + H2 predicate test, acceptance row 5 |
| R-03 | Fixed in both directions | unit + H2 predicate tests, acceptance rows 6–8 |
| Same pattern elsewhere | Pet and HospitalBusinessHours fixed; HospitalSchedule toggles assessed and left as-is with rationale | acceptance rows 9–10 |
| D-01 | Fixed, plus HOSP-T002 correction | docs diff in the patch |

## Receiver instructions

Verify the tree against `HANDOFF-002-SHA256.txt` or apply `HANDOFF-002.patch` onto f0faba9. To reproduce: build `bootJar`, create `daipetto_review_1003`, start the jar with `--spring.profiles.active=local --DB_NAME=daipetto_review_1003 --server.port=18081`, set `BASE`/`DB`, then run smoke → reviewer scripts → `fix-acceptance.mjs` in that order, resetting between runs as in `scripts/README.md`. Source write ownership returns to no one with this round.
