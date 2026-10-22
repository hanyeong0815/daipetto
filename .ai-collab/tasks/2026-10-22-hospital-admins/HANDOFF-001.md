# Handoff 001: 2026-10-22-hospital-admins

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-22T19:22+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: review only
- Source base SHA: `71c0fe8` (branch `feat/spring/hospital-admins`, created from `feat/spring/user-management`, previous round ACCEPTED)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD`
- Checkout/branch and dirty state: `E:\workspace\daipetto`; new files intent-added; unrelated untracked `.vscode/` excluded
- Dirty snapshot: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs spring-api frontend`) and `HANDOFF-001-SHA256.txt` (changed files + `scripts/` + the patch)

## Change summary

Backend:

- V15 `hospital_admins`: id, user_id FK users, hospital_id FK hospitals, created_at, unique (user_id, hospital_id), index on hospital_id. The unique constraint is also declared on `HospitalAdminEntity` so the H2 test schema has it.
- Domain `HospitalAdmin`, `HospitalAdminSummary`, port `HospitalAdminRepository` (exists/save/delete→boolean/deleteAllByUserId/findHospitalIdsByUserId/findAdminsByHospitalId). The adapter uses `saveAndFlush` and translates `DataIntegrityViolationException` to HOSPITAL-007. The admin list joins users and excludes soft-deleted users.
- `HospitalAccessChecker.checkManageable(userId, hospitalId)`: SYSTEM_ADMIN passes; HOSPITAL_ADMIN must be linked; anything else → 403 HOSPITAL-005; missing user → AUTH-005. It reads the current DB role, like the JWT filter. Called first by UpdateHospital, CreateHospitalSchedule, Block/UnblockHospitalSchedule, Create/Update/DeleteHospitalBusinessHours. Approve/Reject/CompleteReservation call it right after loading the reservation, with the reservation's hospital. These use cases now take the acting `userId` first; the controllers pass `extractUserId(authentication)`.
- New: `GET /admin/hospitals/mine` (SYSTEM_ADMIN: all non-deleted incl. SUSPENDED; HOSPITAL_ADMIN: linked), `GET/POST/DELETE /admin/hospitals/{id}/admins` (SYSTEM_ADMIN; POST 201), and `GET /admin/reservations?hospitalId=&status=&date=` (class-level `@PreAuthorize` on `ReservationAdminController`; JPQL joins schedule, user and pet; `cast(:date as LocalDate) is null` for PostgreSQL null params; order by date, start, id; excludes deleted reservations; keeps the names of pets deleted later).
- `AssignHospitalAdminService` takes `lockForSessionUpdate(userId)` before reading the role. Without it, a demotion committing between the role read and the insert left a link on a ROLE_USER (docs/12 §3-17; I found this while writing the acceptance script, before review).
- `ChangeUserRoleService` deletes all links when the new role is not HOSPITAL_ADMIN.
- HospitalErrorCode HOSPITAL-005 (403) / 006 (400) / 007 (409) / 008 (404). `GlobalExceptionHandler`: `MissingServletRequestParameterException` → 400 VALIDATION-001 (was 500).

Frontend:

- `AdminReservationPage` (SC-013) replaces the mock data. It loads managed hospitals, shows a select only when there are 2+, and has per-status counts, a date filter with 「全日程」, and status chips (client-side, so the counts stay meaningful). Approve/reject on REQUESTED and complete on APPROVED, each behind a confirm. It reloads after every action. A stale-response guard, retry on load failure, and an empty-affiliation message are included.
- `AdminHospitalPage` (SC-014) selects from `/admin/hospitals/mine` instead of the public list, so it includes suspended hospitals marked 「（停止中）」, and auto-selects a single hospital. It reloads the list after save, and after suspend it keeps the selection and refreshes the detail. A keyed `HospitalAdminSection` (SYSTEM_ADMIN) lists, links and unlinks admins; candidates are the first 100 ACTIVE HOSPITAL_ADMIN users minus the already-linked ones. The 「hospital_admins未実装」 note is removed.
- API wrappers and types (`ManagedHospital`, `HospitalAdminUser`, `AdminReservation` with `PetSpecies`).

## Acceptance evidence (all executed by Claude Code, 2026-10-22)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew test --offline` after the last Java edit | JDK 17, H2 | exit 0 — 301 tests, 0 failures/errors/skips (281 before) |
| AC-02..AC-06 unit/slice | `HospitalAdminServiceTest` (checker by role/link, managed lists, assign order lock→read→save, failures, unassign), `GetAdminReservationListServiceTest`, the 4 admin controller tests (403, 201, 400s), updated hospital/schedule/business-hours/transition service tests, `AdminUserServiceTest` (demotion removes links) | Mockito / WebMvc | PASS |
| AC-04/AC-06 SQL | `HospitalAdminPersistenceAdapterTest` (link/query/unlink, deleted user hidden, duplicate → HOSPITAL-007), `ReservationAdminQueryTest` (filters, null params, order, deleted excluded, deleted pet named) | H2 `@DataJpaTest` | PASS |
| AC-01..AC-06 on PostgreSQL | `scripts/hospital-admins-acceptance.mjs`, bootJar built after the last Java edit | docker `postgres:16.9`, fresh DB `daipetto_ha_1022` (Flyway V1→V15), port 18081 | 13/13 |
| AC-05 race | same script, HA-13 | same | link request observed blocked in `pg_stat_activity` on the user row lock held by a psql demotion; after commit 400 HOSPITAL-006, 0 links |
| Regression | `../2026-09-13-implementation-review/scripts/e2e-api.mjs` on the same backend | same | 12/12 |
| Frontend build | `frontend/`: `npm run build` after the last edit | Node 24.17.0 | exit 0 |
| AC-07 browser | Vite 5173 → 18081, fixture sessions injected as refresh tokens (no password typed), `window.confirm` stubbed | built-in browser | Hospital admin: SC-013 shows the linked hospital, counts 1/0/1, two cards; 承認 → 予約確定 + 「診療完了にする」, confirm text includes date/time/user/pet; date 2026-12-01 → request `&date=2026-12-01`, one card. SC-014 auto-selects the single hospital and shows no admin section. System admin: select shows 3 hospitals with 「（停止中）」; admin section lists HappyVet; adding OtherVet and then 解除 both work; 「この病院を停止する」 keeps the selection, shows 停止中 and updates the select label. Unlinked hospital admin: both screens show the 「紐付けを依頼」 message. No console errors. |

Scratch DB dropped; temporary launch configuration reverted.

## Limitations

- One extra user lookup per hospital-managing call (the checker reads the user again; the JWT filter already did). Accepted for clarity.
- Authorization is check-then-act. An operation that passed the check at the same moment as an unlink still completes; the next request is rejected.
- SC-014 candidates are capped at 100 ACTIVE hospital admins (no search box). Suspended hospital admins are not offered.
- The SPA does not refresh a demoted admin's stored menu; APIs return 403 (unchanged from the previous round).
- `WebMvcTest` slices exclude the JWT filter, so `@PreAuthorize` role enforcement is evidenced by the PostgreSQL HTTP run.
- Unit 6 (slot auto-generation scheduler) is out of this round.

## Documentation

- docs/06: §3 table V15, §11 constraints/FKs and operation note, §17 indexes.
- docs/07: new §2-8 (affiliation rule, which APIs, check order), §9-5/9-6/9-7 text, new §9-8, §11-6 link deletion, new §11-8..11-11, §14 role table.
- docs/05: SC-013/SC-014 rewritten to the implemented screens; screen list roles.
- docs/09: §4-3 table, new §4-5.
- docs/02: FR-HOSPITAL-002, FR-RESERVATION-001.
- docs/11: ROLE-T003/T004, HADM-T001..T009.
- docs/12: §3-16 missing-param note, new §3-17 (link vs demotion race).
- TODO.md, AGENTS.md (§5 codes, §7 V15/next V16 and affiliation rule, §8 301, §10).

## Response to previous review

None for this task. The previous task (2026-10-21-user-management) closed ACCEPTED with no open findings.

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. If the default Gradle home is inaccessible, use `GRADLE_USER_HOME=spring-api/.gradle/review-home` with `--offline`. PostgreSQL steps are in `scripts/README.md` (new scratch DB only, port other than 8080). Write findings to `REVIEW-001.md` in this directory.
