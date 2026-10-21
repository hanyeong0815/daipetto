# Handoff 001: 2026-10-05-vaccination

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-21T23:20+09:00 (first published 2026-10-05T23:40+09:00)
- Revision: revised before consumption. On 2026-10-21 the user asked to finish all Spring work and chose to have Claude Code implement the former practice gaps (cancel, delete). This revision adds those two endpoints and the frontend screens. No earlier version of this round was reviewed; the patch and manifest below replace the first version's.
- Task specification: TASK.md
- Recipient: Codex
- Requested action: review only
- Source base SHA: `6e18153cd7010c73a5771285b045cb3bdc3e55aa` (branch `feat/spring/vaccination`, created from `feat/react-api-integration`)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD` against the base SHA
- Checkout/branch and dirty state: `E:\workspace\daipetto`, `feat/spring/vaccination`; new files marked intent-to-add (`git add -N`); unrelated untracked `.vscode/` excluded
- Dirty snapshot manifest and patch location: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`, 48 files) and `HANDOFF-001-SHA256.txt` (those 48 plus this task's `scripts/`). Freeze these files during review.

## Policy changes in this round (user decisions, 2026-10-21)

- The Vaccination cancel/delete endpoints are no longer practice gaps: review them as normal scope (`.codex/REVIEW.md` "do not implement the user's practice exercises" no longer applies to them).
- The same request leaves no practice gaps for the rest of this round.

## Change summary

Backend (first version, unchanged):

- `V11__create_vaccinations.sql`: `vaccinations` (docs/06 §14) + indexes `(pet_id)`, `(status, scheduled_date)`; adds FK `fk_notifications_vaccination` deferred since V10.
- Domain `vaccination`: `Vaccination` (immutable; `create`, `complete(date)` SCHEDULED-only, `reschedule(date)` CANCELLED-only), `VaccinationStatus`, `VaccinationReminderTarget`, `VaccinationErrorCode` (001 404 / 002 403 / 003 409 / 999), `VaccinationRepository`.
- Application: `VaccinationService` (create/list), `CompleteVaccinationService`, `RescheduleVaccinationService`, `SendVaccinationRemindersService` (7-day window, writes via `NotificationRepository`).
- Persistence: `markCompleted` / `reschedule` are `@Modifying` updates with expected status + `deleted_at IS NULL` in WHERE, returning affected==1. `findReminderTargets` JPQL joins `PetEntity` (owner, name, pet not deleted) and excludes vaccinations having a notification with `created_at >= v.updated_at`.
- `VaccinationReminderScheduler` (`@EnableScheduling`, cron `0 0 9 * * *`, zone Asia/Tokyo) passes `LocalDate.now(ServerTime.ZONE_ID)`.
- `Notification.forVaccination(userId, vaccinationId, message)`; comment update in `NotificationType`.

Added in this revision:

- `PATCH /api/v1/vaccinations/{id}/cancel` (docs/07 §6-9): `Vaccination.cancel()` (SCHEDULED-only) → `CancelVaccinationService` → `VaccinationRepository.cancel`, a conditional UPDATE `where id and status = SCHEDULED and deleted_at is null` that also sets `updated_at` (so a later reschedule is reminded again). 0 rows → VACCINATION-003.
- `DELETE /api/v1/vaccinations/{id}` (docs/07 §6-11): `DeleteVaccinationService` → `VaccinationRepository.softDelete`, `update ... set deleted_at where id and deleted_at is null`. 0 rows (deleted concurrently after the read) → VACCINATION-001. Any status can be deleted.
- Both follow the existing owner check through the pet (VACCINATION-002) and one Service per UseCase.
- Frontend: `types/vaccination.ts`, `api/vaccination.ts` (6 calls), new `VaccinationPage` at `/pets/:petId/vaccinations` (docs/05 SC-016). It lists, registers, completes (date input defaulting to today JST, `max` today), cancels, reschedules (new date) and deletes. The list request is ignored if the pet changes before it resolves. After a failed action it shows the API message and reloads, so a row changed elsewhere shows its real state. `PetDetailPage` gains the SC-006 「ワクチン予定」 panel (next three SCHEDULED, days left, link). `DetailLayout` titles the page 「ワクチン予定」.

## Acceptance evidence

All executed by Claude Code on 2026-10-21 unless marked 2026-10-05.

| Criterion | Test/check | Working directory and command | Environment | Exit code/outcome |
|---|---|---|---|---|
| AC-01..AC-05 + cancel/delete | Unit/WebMvc/H2 tests | `spring-api/`: `./gradlew clean test` | Windows 11, JDK 17, H2 test profile | 0 — 253 tests, 0 failures/errors/skips (239 before this revision; +8 service, +4 controller, +2 H2 adapter) |
| Cancel/delete SQL predicates | `VaccinationPersistenceAdapterTest.cancel_updates_only_scheduled_row`, `soft_delete_only_active_row` | same | H2 `@DataJpaTest` | PASS: cancel only from SCHEDULED, advances `updated_at`; second delete returns false; deleted row absent from list and reminder targets |
| HTTP + PostgreSQL | `scripts/vaccination-acceptance.mjs` | repo root, `BASE=http://localhost:18081 DB=daipetto_vac_1021` | boot jar, local profile, docker `postgres:16.9`, fresh scratch DB (dropped afterwards) | 0 — 13/13 (see below) |
| AC-03..AC-06 (2026-10-05) | temporary `@SpringBootTest` | `spring-api/`: `DB_NAME=daipetto_vac ./gradlew test --tests '*TmpVaccinationPostgresCheck'` | throwaway DB `daipetto_vac` | 0 — V1..V11 applied, `ddl-auto: validate`, `TMP_PG_CHECK_OK` (`scripts/TmpVaccinationPostgresCheck.java.txt`) |
| Frontend build | `npm run build` (`tsc -b && vite build`) | `frontend/` | Node 24.17.0 | 0 (after the last edit). `npm run lint`: ESLint not installed/configured (pre-existing) |
| Browser | manual, Vite on 5173 → backend 18081, same scratch DB | built-in browser | Chromium pane | see below |

`vaccination-acceptance.mjs` results:

```text
VAC-T001 register and list in scheduled-date order          200 2:2026-11-01,1:2026-11-20
VAC-T002 other user cannot cancel                           403 VACCINATION-002
VAC-T003 SCHEDULED -> CANCELLED                             200 CANCELLED/-/false
VAC-T004 cancelling a CANCELLED row                         409 VACCINATION-003
CANCELLED -> SCHEDULED with the new date                    200 SCHEDULED/2026-12-01
SCHEDULED -> COMPLETED with completedDate                   200 COMPLETED/2026-10-20/false
cancelling a COMPLETED row                                  409 VACCINATION-003
other user cannot delete                                    403 VACCINATION-002
VAC-T005 delete sets deleted_at and hides the row           200 COMPLETED/2026-10-20/true listed=false
deleting a deleted row                                      404 VACCINATION-001
race: cancel behind a committed completion                  409 VACCINATION-003, row COMPLETED
race: delete behind a committed delete                      404 VACCINATION-001
race: cancel behind a committed delete                      409 VACCINATION-003, row stays deleted
```

Each race holds the competing change uncommitted in psql and waits until `pg_stat_activity` shows the HTTP request blocked on `update vaccinations` before committing (the script throws if it never blocks).

Browser (fixture session injected into `localStorage` as a refresh token; no password typed into the page):

- Pet detail showed the panel: フィラリア予防 2026-10-24（あと3日）, 混合ワクチン 2026-12-01（あと41日）.
- The vaccination page listed all states with the right buttons per status.
- Complete (today), reschedule of a CANCELLED row (2026-11-15), cancel, delete, and add (狂犬病 2026-11-02) each updated the list.
- Stale-row case: the row was cancelled via SQL behind the page, then 「中止」 was clicked. Before the fix, the page showed 「現在の状態では変更できません。」 but kept the stale row. After the fix, the same message shows and the row reloads as 中止 with 「再登録」.
- No console errors. `window.confirm` was stubbed for automation.

## Limitations

- Cancel losing a race to a concurrent delete returns 409 VACCINATION-003, not 404; the row stays deleted. Documented by the acceptance script; judged acceptable because the client reloads the list after any error.
- Scheduler cron trigger itself is not tested; only the use case it calls (2026-10-05 PG check covers extraction and dedupe).
- Reminder dedupe relies on every state-changing write advancing `updated_at`; complete, cancel and reschedule all do.
- A notification can be created for a vaccination completed concurrently with the 09:00 run (no row lock); judged benign.
- Single-instance Scheduler assumption: multiple app instances would duplicate reminders.
- `scheduledDate` on create/reschedule may be in the past (no validation in spec); such rows are never reminded.
- No Android/iOS runtime check of the new screen.

## Documentation

- docs/07: §6-5 summary table (all implemented), §6-9/§6-11 headings and behaviour.
- docs/08: §7-3 cancel row link.
- docs/05: SC-016 added (list, §10-1); SC-006 operation row.
- docs/11: VAC-T001..T005.
- docs/02: stale 「（未実装）」 on FR-HEALTH-001/FR-NOTI-001 and the "reject API unimplemented" note removed. All three features exist.
- TODO.md: practice-gap items marked done by user decision; Vaccination frontend section.
- AGENTS.md: §8 count 253, §10 vaccination status and practice gaps.
- Earlier revision: docs/06 (§3 V11, §15 FK note, §17 indexes), docs/07 §6-5..§6-11 and §10 reminder rules, docs/08 §7-3/§11-1, docs/11 SCHD mapping.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| None for first round | N/A | N/A | N/A |

## Receiver instructions

Review the working tree on `feat/spring/vaccination` in this checkout, or apply `HANDOFF-001.patch` onto `6e18153` in a separate worktree. Confirm hashes against `HANDOFF-001-SHA256.txt`. Use a scratch database for any PostgreSQL run (`scripts/README.md`); do not use `daipetto` or port 8080. Write findings to `REVIEW-001.md` in this directory.
