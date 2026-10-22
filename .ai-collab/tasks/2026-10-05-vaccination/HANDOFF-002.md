# Handoff 002: 2026-10-05-vaccination

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-21T23:30+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: re-review of R-01..R-04
- Consumed review: [REVIEW-001.md](REVIEW-001.md) (not edited)
- Source base SHA: `6e18153cd7010c73a5771285b045cb3bdc3e55aa` (branch `feat/spring/vaccination`)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD` against the base SHA
- Checkout/branch and dirty state: `E:\workspace\daipetto`, `feat/spring/vaccination`; new files intent-added; unrelated untracked `.vscode/` and the reviewer's `spring-api/.gradle/review-home` cache (gitignored) excluded
- Dirty snapshot: `HANDOFF-002.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`) and `HANDOFF-002-SHA256.txt` (changed files + this task's `scripts/` + the patch). HANDOFF-001 and its manifest are preserved; its manifest no longer matches by design.

## Response to REVIEW-001

| ID | Disposition | Change | Evidence |
|---|---|---|---|
| R-01 [P2] | Fixed | `VaccinationPage` is now a thin wrapper that renders `VaccinationList key={petId}`, the same pattern as the F-02 `ReservationPage` fix. A pet switch remounts the list, so rows, the open date action and the form never carry over, whether the next load succeeds or fails. `HealthRecordPage` had the identical defect and gets the same wrapper. A failed latest load now sets `loadFailed`, which hides the (possibly stale) actionable list and the empty-state text and shows 「再試行」. | Browser, see below |
| R-02 [P2] | Fixed | `PetDetailPage` keeps `upcomingFailed` separate from the result. A failure shows 「ワクチン予定を取得できませんでした。」 + 「再試行」 instead of 「予定はありません。」. Empty-success and populated-success are unchanged. The same "failure looks empty" pattern in the two list pages is covered by `loadFailed` above. | Browser, see below |
| R-03 [P2] | Fixed (Codex's second option: tie dedupe to the exact schedule) | New column `vaccinations.reminded_date` (`V12__add_vaccination_reminded_date.sql`). For each extracted target, the reminder service first runs `markReminded`: `update ... set reminded_date = :d where id and status = SCHEDULED and deleted_at is null and scheduled_date = :d and reminded_date is null`. It creates the notification only if 1 row changed, in the same transaction. Extraction now uses `reminded_date is null` instead of the `created_at >= updated_at` comparison. Reschedule sets `reminded_date = null`. The claim UPDATE and the transitions serialize on the row lock and are re-evaluated after waiting (READ COMMITTED), so a cancel/reschedule committed after extraction makes the claim fail and the new date stays eligible. Correctness no longer depends on clock order. V12 backfills `reminded_date = scheduled_date` where the old rule said "already notified", for databases that applied V11 earlier. | Unit + H2 + PostgreSQL, see below |
| R-04 [P3] | Fixed | Page copy: 「接種予定日が7日以内になると、朝9時に一度だけ通知でお知らせします。予定日を変更した場合は、新しい予定日で改めてお知らせします。」 docs/05 SC-016 says the same (one reminder per scheduled date, again after reschedule). | — |

Design note on R-03: the alternative was to lock-read in every transition and compute `updated_at` after the lock. It keeps the timestamp rule but needs every writer to follow the lock order and depends on application clock ordering. `reminded_date` follows AGENTS §3 rule 5 (state claimed by a conditional UPDATE) and makes the deduplicated thing explicit: the scheduled date that was reminded.

## Acceptance evidence (all executed by Claude Code, 2026-10-21)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew clean test` | JDK 17, H2 | exit 0 — 256 tests, 0 failures/errors/skips (was 253) |
| R-03 unit | `VaccinationServiceTest.send_reminders_skips_target_changed_after_extraction`; reminder test now stubs `markReminded` | Mockito | PASS: a target whose claim fails produces no notification, others still do, count = sent |
| R-03 / SCHD-T005 SQL | `VaccinationPersistenceAdapterTest`: `find_reminder_targets_skips_already_reminded`, `rescheduled_after_reminder_is_extracted_again`, `mark_reminded_fails_for_target_rescheduled_after_extraction` | H2 `@DataJpaTest` | PASS: second claim fails; cancel+reschedule after a reminder re-extracts with the new date; extract D1 → cancel+reschedule D2 → claim(D1) false, D2 still extracted, claim(D2) true |
| R-03 on PostgreSQL, real interleaving | `scripts/TmpReminderPostgresCheck.java.txt` copied into the test tree, `DB_NAME=daipetto_vac_r03 ./gradlew test --tests '*TmpReminderPostgresCheck'`, then removed from `src` | docker `postgres:16.9`, fresh scratch DB, local profile | exit 0, `TMP_R03_PG_CHECK_OK`. Flyway applied V1..V12, schema validation passed. The claim UPDATE was observed blocked in `pg_stat_activity` behind an uncommitted reschedule. After commit the run sent 0, the notification count was 0 and `reminded_date` stayed NULL. The next run sent 1 for D2 (message contains D2) and a third run sent 0. |
| V12 backfill from a V11 database | jar `--spring.flyway.target=11` on scratch DB `daipetto_vac_bf`; seed 3 vaccinations + 2 notifications via psql; restart without target | same | V12 applied (1 migration). Notified after its last change → `reminded_date=2026-10-25`; notified before a later reschedule → NULL; never notified → NULL |
| HTTP regression | `scripts/vaccination-acceptance.mjs` on the fixed jar | scratch DB `daipetto_vac_1021`, port 18081 | 13/13 |
| Frontend build | `frontend/`: `npm run build` | Node 24.17.0 | exit 0 after the last edit |
| R-01 browser | Vite 5173 → 18081. Loaded `/pets/1/vaccinations` (rows with 「接種完了」), then switched in-app via `history.pushState` + `popstate` to `/pets/999/vaccinations` (403) | built-in browser | Only the API error and 「再試行」; no pet-1 rows or buttons. Switching to `/pets/2/vaccinations` showed only pet 2's row. 2 requests per visit (StrictMode dev double-invoke), no loop. |
| R-02 browser | `vaccinations` renamed in the scratch DB to force a 500 while the pet detail succeeds; open `/pets/1` | same | Panel showed 「ワクチン予定を取得できませんでした。」 + 「再試行」. After renaming back, 「再試行」 showed 「混合ワクチン 2026-12-01（あと41日）」. |

All scratch databases were dropped and the temporary launch configuration reverted. `daipetto` and port 8080 were not used.

## Limitations

- The browser checks are manual, and so is the R-01 switch: a `history.pushState` + `popstate` in-app navigation, not a link click. There is no automated frontend test (the project has no frontend test runner).
- `HealthRecordPage` got the same keyed wrapper and failed-load state. It was outside REVIEW-001's scope, but it is the same defect class. Its create/edit/delete flows were not re-run in the browser this round; the build passes.
- Reminders sent concurrently with a completion: the claim requires SCHEDULED, so a completion committed first makes the claim fail; a claim committed first means the reminder was valid when sent.
- Still single-instance Scheduler by assumption, but two concurrent runs can no longer double-send: the claim admits one.
- Unchanged from HANDOFF-001: past `scheduledDate` allowed (never reminded); cancel losing to a concurrent delete returns 409; cron trigger itself untested.
- Codex's leftover `spring-api/.gradle/review-home` (3.6 GB, gitignored) is kept so the next review can reuse it as `GRADLE_USER_HOME`; Claude Code will remove it at the end of the round.

## Documentation

docs/05 (SC-016 copy, SC-006 failed-load row), docs/06 (`reminded_date` column, inventory V12), docs/07 (§6-9, §6-10 reschedule clears the marker, §10 reminder rule), docs/08 (§11-1 condition), docs/11 (SCHD mapping incl. R-03), docs/12 (§3-14 timestamp-comparison dedupe lesson), TODO.md (reminder rule, pet-fields renumber target V13+), AGENTS.md (§7 V12 / next V13, §8 256, §10 dedupe wording).

## Receiver instructions

Verify `HANDOFF-002-SHA256.txt` from the repository root. If Gradle cannot use the default user home, `spring-api/.gradle/review-home` from your previous run is still present. PostgreSQL steps are in `scripts/README.md`; use only new scratch databases and ports other than 8080. Write the result to `REVIEW-002.md`; do not edit REVIEW-001 or HANDOFF-001.
