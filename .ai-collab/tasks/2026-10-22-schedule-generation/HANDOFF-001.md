# Handoff 001: 2026-10-22-schedule-generation

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-22T20:54+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: review only
- Source base SHA: `019ea93` (branch `feat/spring/schedule-generation`, created from `feat/spring/hospital-admins`, previous round ACCEPTED)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD`
- Checkout/branch and dirty state: `E:\workspace\daipetto`; new files intent-added; unrelated untracked `.vscode/` excluded
- Dirty snapshot: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs spring-api frontend`) and `HANDOFF-001-SHA256.txt` (changed files + `scripts/` + the patch)

## Change summary

Backend:

- `HospitalBusinessHours.slotsOn(date)` (domain): splits [open, breakStart), [breakStart, breakEnd), [breakEnd, close) independently by the slot length. Break slots are `BLOCKED`, remainders dropped. Arithmetic is in minutes since midnight because `LocalTime.plusMinutes` wraps at 24:00 and could loop forever. A stored break is clipped to opening hours. Invalid stored values (slot ≤ 0, open ≥ close) yield no slots.
- `HospitalBusinessHours.create/update` now validate: open < close; slot 5..240; break both-or-none, within opening hours, start < end. Violations → HOSPITAL-009 (400). Previously the API accepted 0-minute slots and reversed times (docs/12 §3-18).
- `HospitalScheduleGenerator.generate(hospitalId, from, to)` is a separate `@Component` with `@Transactional` so the monthly loop gets one transaction per hospital. It first locks the hospital row with `HospitalRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, non-deleted, else HOSPITAL-001). It then loads business hours and `HospitalScheduleRepository.findDatesWithSchedules` (distinct dates in range). Days without hours are skipped silently; days with any slot are skipped and counted. It saves with `saveAll` and returns `{createdSlots, generatedDates, skippedDates}`.
- `HospitalScheduleGenerationService`:
  - Manual use case `execute(userId, hospitalId, YearMonth)`: `checkManageable` first, then this month..+12 (JST) or HOSPITAL-010 (400). This month starts from today; other months from the 1st.
  - Monthly use case `execute(YearMonth)`: ACTIVE hospitals from `findAllForManagement`. Per-hospital `RuntimeException`s are logged and the loop continues. Returns the summed `createdSlots`.
- `HospitalScheduleGenerationScheduler`: `@Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Tokyo")` for next month (`@EnableScheduling` already on the vaccination scheduler).
- `POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate` `{ "month": "YYYY-MM" }` → 200 with the counts. HOSPITAL_ADMIN/SYSTEM_ADMIN; missing or unparsable month → 400 VALIDATION-001.

Frontend:

- SC-014 「予約枠の自動生成」 section: `<input type="month">` (default next month, min this month, max +12, from `todayInJapan()` with a new `addMonths` helper) and 「生成する」.
  - Message: created count and days. If nothing was created, it says either "every open day already has slots" or "register business hours".
  - The schedule list is refreshed via `refreshIfSelected` (the REVIEW-001 R-01 guard of the previous task).
- `hospitalApi.generateSchedules`, `ScheduleGenerationResult` type.

## Acceptance evidence (all executed by Claude Code, 2026-10-22)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew test --offline` after the last Java edit | JDK 17, H2 | exit 0 — 316 tests, 0 failures/errors/skips (301 before; +5 domain, +5 service, +3 controller, +2 H2) |
| AC-01/AC-06 | `HospitalBusinessHoursTest` (break split, per-segment remainder, midnight, invalid stored values, create/update validation) | unit | PASS |
| AC-02/03/04/05 | `HospitalScheduleGenerationServiceTest` (lock → dates → saveAll order, 28/8/1 counts for a November fixture, missing hospital, ranges incl. today/+12, HOSPITAL-010/005, monthly ACTIVE-only + failure continue) | Mockito | PASS |
| SQL | `HospitalSchedulePersistenceAdapterTest` (distinct dates in range per hospital incl. BLOCKED, saveAll, `findByIdForUpdate` excludes deleted) | H2 `@DataJpaTest` | PASS |
| Controller | `HospitalScheduleAdminControllerTest` (200 with counts, 400 for `{}` / `2026-13` / `2026/11`, HOSPITAL-010 → 400) | WebMvc | PASS |
| AC-01..AC-04, AC-06 on PostgreSQL | `scripts/schedule-generation-acceptance.mjs`, bootJar after the last Java edit | docker `postgres:16.9`, fresh DB `daipetto_gen_1022`, port 18081 | 9/9 (incl. GEN-08 concurrency and GEN-09 lock wait observed in `pg_stat_activity`) |
| Regression | `../2026-09-13-implementation-review/scripts/e2e-api.mjs` on the same backend | same | 12/12 |
| Frontend build | `frontend/`: `npm run build` after the last edit | Node 24.17.0 | exit 0 |
| AC-07 browser | Vite 5173 → 18081, hospital-admin fixture session injected (no password typed) | built-in browser | Month input default 2026-11, min 2026-10, max 2027-10. Generating 2027-05 shows 「2027-05の予約枠を34件作成しました（9日分）」 and the list contains 34 slots for 2027-05; again shows 「2027-05は診療日すべてに既に予約枠があります（9日）」 |

Scratch DB dropped; temporary launch configuration reverted.

## Limitations

- The cron trigger itself is not exercised; the monthly use case is unit-tested and uses the same generator as the PostgreSQL-tested manual path.
- Day-level skip: a day with one manual slot gets no generated slots at all.
- Manual single-slot creation (8-4) does not take the hospital lock and does not check overlap (pre-existing behavior). Its FK check waits for a running generation, but it can still add an overlapping slot to a generated day.
- After deployment, the current and next month must be generated manually once (the cron only creates the following month on the 1st).
- No upper bound on how many slots a month may produce beyond the validation (5-minute slots over 24 hours give at most 288 per day).

## Documentation

- docs/07: §2-8 table, §8-4 note, §8-7 input rules + HOSPITAL-009 (also §8-9), new §8-11, §14 role table, §19 row removed.
- docs/04 §14 implemented scheduler; docs/06 §18 implementation note; docs/05 SC-014 row; docs/08 §11 note; docs/09 §15-1 row.
- docs/02 FR-HOSPITAL-002; docs/11 GEN-T001..T006; docs/12 §3-18.
- TODO.md, AGENTS.md (§5 HOSPITAL-009/010, §8 316, §10 entry, Not started, branch stack).

## Response to previous review

None for this task. The previous task (2026-10-22-hospital-admins) closed ACCEPTED in REVIEW-002 with no open findings.

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. If the default Gradle home is inaccessible, use `GRADLE_USER_HOME=spring-api/.gradle/review-home` with `--offline`. PostgreSQL steps are in `scripts/README.md` (new scratch DB only, port other than 8080). Write findings to `REVIEW-001.md` in this directory.
