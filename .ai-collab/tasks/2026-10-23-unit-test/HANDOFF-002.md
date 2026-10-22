# Handoff 002: 2026-10-23-unit-test

- Author: Claude Code (test design: main session Opus 5.5; execution partly by Sonnet 5.5 subagents, per user instruction)
- Publication: ready
- Timestamp: 2026-10-23T05:16+09:00
- Task specification: TASK.md
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: final verification. Check the response to REVIEW-001, and check that the final evidence matches the specs.
- Consumed review: REVIEW-001.md (not edited; a copy is at `test/reviews/REVIEW-001.md`)
- Source base SHA: `7ee0db6` (develop) on branch `test/unit-test`
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD` plus untracked `test/`, `spring-api/src/test/java/.../infrastructure/scheduler/`
- Dirty snapshot: `HANDOFF-002-SHA256.txt` (tracked changes + new test classes + every file under `test/` except `test/evidence/coverage/html/`). `docs/report/` (untracked beginner reports from an earlier task) and the user's `.claude/` tool installation are not part of this task.

## Response to REVIEW-001

| ID | Disposition | Change / evidence |
|---|---|---|
| G-01 | Added | JUnit `infrastructure.scheduler.HospitalScheduleGenerationSchedulerTest`: next JST month passed once (before/after tolerance for month boundary); `@Scheduled` cron/zone checked by reflection. No Clock injection was added, so the rollover is not simulated (the logic is `YearMonth.now(JST).plusMonths(1)`). |
| G-02 | Added | JUnit `infrastructure.scheduler.VaccinationReminderSchedulerTest`: today (JST) once; cron `0 0 9 * * *` / Asia/Tokyo |
| G-03 | Added | API UT-03-012 (PATCH weight -0.01 / 1000 → 400 VALIDATION-001, unchanged) |
| G-04 | Added | API UT-04-014 (reschedule missing → 404 VACCINATION-001); JUnit `reschedule_vaccination_fail_when_transition_lost` (VACCINATION-003) |
| G-05 | Added | API UT-09-013 (cancel missing → RESERVATION-006); UT-10-009 (admin complete missing → RESERVATION-006, row-count snapshot unchanged) |
| G-06 | Added | API UT-12-007 (activate self → USER-005, missing → USER-003) |
| G-07 | Added | API UT-13-008: all 15 routes with ROLE_USER → 403 AUTH-005; the 4 system-admin-only routes also with ROLE_HOSPITAL_ADMIN; before/after snapshot of row counts and states of 9 table aggregates unchanged |
| G-08 | Added | API UT-07-009 (PATCH/DELETE business hours of an unlinked hospital and a nonexistent hospital → 403 HOSPITAL-005, unchanged); UT-08-010 (unblock unlinked → 403, still BLOCKED) |
| G-09 | Added | API UT-07-010 (PATCH reversed hours / 241 min / break outside → 400 HOSPITAL-009, unchanged) |
| G-10 | Added | JUnit: RefreshTokenService missing user → AUTH-001 (no lock/revoke/save); Update/Delete health record with empty pet → HEALTH-002; complete/cancel/reschedule/delete vaccination with empty pet → VACCINATION-002; create reservation with absent schedule → RESERVATION-002 and absent hospital → RESERVATION-009; get list/detail with missing hospital/schedule → RESERVATION-002 and pet missing even including deleted → RESERVATION-004. All verify no write. No discrepancy with the expected codes. |
| G-11 | Added; found D-008 | JUnit `HospitalControllerTest.unexpected_exception_returns_server_error`: 500, SERVER-001, data absent/null, Japanese message, body without the exception text. The message was the English `server error` and was changed to Japanese (D-008, `GlobalExceptionHandler`). |
| R-01 | Fixed | `lib.mjs` `maskText` masks JWTs in recorded SQL, DB results, check values and response strings (the executed SQL is unchanged). Final evidence regenerated; scan of `test/evidence`, `test/spec`, `test/*.md` and `test/reviews` for full JWTs and the fixture password: 0 files. |
| R-02 | Fixed | `test/README.md` written: plan, scope, criteria, environment, commands from the repository root (DB drop/create, backend start, runner, spec generation, cleanup), folder layout. The review link is `test/reviews/`. |
| Weak checks | Fixed | UT-13-004/005/007 now assert COMMON-001/002/003 and the error envelope; UT-10-004/005 assert notification count/recipient/type |

## Other changes since HANDOFF-001

- Screen unit tests UT-14-001..033 (`test/spec/UT-14_screens.md`, `test/evidence/screen/`). Defects found by them:
  - **D-007** (SC-008 search box sent only `keyword`, so area search returned 0) → `HospitalSearchPage.tsx` now has a separate area field.
  - **D-009** (SC-002 replaced API validation/duplicate messages with a fixed text) → `RegisterPage.tsx` shows the API message. Re-tested in UT-14-023 and UT-14-033.
- Docs: docs/10 (test dependencies incl. JaCoCo; the stale migration list V1..V7 updated to V1..V15), docs/11 (pointer to `test/`), AGENTS.md (§8 342 + `test/`, §10 entry, branch status), TODO.md.

## Final evidence (2026-10-23)

| Check | Command / where | Outcome |
|---|---|---|
| JUnit | `spring-api/`: `./gradlew test --offline` (run by a Sonnet subagent after the last Java test edit; no production Java change afterwards) | 342 tests, 0 failures/errors/skips |
| API | fresh scratch DB `daipetto_ut`, bootJar rebuilt after the last production edit, backend 18081, `run-api-unit-tests.mjs` (Sonnet subagent) | 134/134 OK; `test/evidence/api/_summary.json` |
| Spec generation | `node test/scripts/build-spec.mjs` | 13 spec files; API 134/134; JUnit 342/342; line 86.9%; branch 76.7% |
| Screens | Vite + backend 18082 on scratch DB `daipetto_ut_screen` (seed script), built-in browser | 33/33 OK (D-007/D-009 fixed and re-tested) |
| Frontend build | `frontend/`: `npm run build` after the last frontend edit | exit 0 |
| Cleanup | scratch DBs dropped, servers stopped | done |

## Limitations

- PostgreSQL and browser runs are implementer-executed (reviewer has no Docker access).
- Schedulers: the cron trigger itself is not waited for; the entry methods and annotations are unit-tested.
- Some screen evidence uses JSON/text where the browser does not render the information (native validation bubble, confirm dialogs, native select popup). A short-lived toast was captured with an in-page timer patch.
- Coverage is reported, not gated (no minimum threshold configured).

## Receiver instructions

Verify `HANDOFF-002-SHA256.txt` from the repository root. JUnit: `GRADLE_USER_HOME=spring-api/.gradle/review-home` with `--offline`. Spot-check the API evidence JSON against the spec rows (verdict, request/response, masking), and the screen spec links (files exist). Write `REVIEW-002.md`; do not edit REVIEW-001 or HANDOFF-001.
