# Review 001: 2026-10-23-unit-test

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-23 (JST)
- Consumed handoff: HANDOFF-001.md
- Reviewed source: branch `test/unit-test`, base/HEAD `7ee0db60e1d42acbf7e11f361a632ba56791791c`, plus frozen dirty snapshot `HANDOFF-001-SHA256.txt`.
- Snapshot verification: **211/211 entries matched initially**, SHA-256 computed from the repository root before inspecting test contents. Final recheck: **210/211 matched**; another writer added D-007 (excluded screen work) to `test/DEFECTS.md` during review. D-001..006 remain unchanged. No other manifest entry changed. See Verification below.
- Verdict: **CHANGES_REQUESTED**
- Scope: all four handoff requests; AC-01/02/03/05 for the API/JUnit package. Screens/UT-14/AC-04 are excluded from this round. PostgreSQL execution is reported-only.

All 52 controller endpoints have an API item. This does not satisfy AC-01 yet: both scheduler entry methods and several endpoint-specific error paths are missing. The independently rerun JUnit suite passes all 321 tests. Existing expectations inspected against docs/07 and code are generally sound; missing assertions and evidence handling still need correction before final evidence.

## Findings

All G findings below are **P2, open**. They concern the test package's explicit acceptance criteria, not requests to implement new product features. Proposed IDs are reservations for new items, not claims that those items exist. Adding JUnit cases instead of HTTP cases is acceptable where noted. Existing section-2 row numbers below refer to `test/spec/UT-xx_*.md`.

### G-01 [P2] Monthly scheduler entry method is never exercised

- File/line: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/scheduler/HospitalScheduleGenerationScheduler.java:18-24`; `test/spec/UT-08_schedule.md:30`.
- Existing evidence: UT-08 section 2 #2 tests `HospitalScheduleGenerationService.execute(month)` with a supplied month. It never calls `generateNextMonth()`. Stored JaCoCo shows the scheduler's method instructions entirely missed.
- Missing behavior: deriving **next month in JST** and forwarding it once, plus the scheduled cron/zone contract.
- Proposed item `UT-08-010` / JUnit scheduler test: call `generateNextMonth()` with a controlled date around month/year rollover; verify exactly one use-case call with next JST month. Verify `@Scheduled(cron="0 0 0 1 * *", zone="Asia/Tokyo")`. No live clock wait is necessary.

### G-02 [P2] Vaccination scheduler entry method is never exercised

- File/line: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/scheduler/VaccinationReminderScheduler.java:21-25`; `test/spec/UT-04_vaccination.md:40`.
- Existing evidence: UT-04 section 2 #4/#20/#21 and #27-35 cover reminder service/query/deduplication behavior. They do not invoke `remind()`; its method instructions are entirely missed in stored JaCoCo.
- Proposed item `UT-04-014` / JUnit scheduler test: at a controlled UTC/JST date boundary, invoke `remind()` and verify exactly one call with today's **JST** date. Check daily 09:00 JST annotation (`0 0 9 * * *`, `Asia/Tokyo`).

### G-03 [P2] Health-record PATCH validation is not tested

- File/line: docs/07:640 (§6-3); `test/spec/UT-03_health-record.md:23-25`; `HealthRecordControllerTest` has only a successful PATCH.
- Missing endpoint/code: `PATCH /api/v1/health-records/{healthRecordId}` -> `400 VALIDATION-001`.
- UT-03-003 tests negative weight on **POST**, not PATCH; service tests bypass request validation.
- Proposed `UT-03-012`: PATCH an owned existing record with `weight=-0.01` and, separately, a value exceeding DECIMAL(5,2). Expect 400/VALIDATION-001 and unchanged stored fields. This catches missing `@Valid` or constraints on the update DTO.

### G-04 [P2] Vaccination reschedule lacks not-found and lost-update tests

- File/line: docs/07:857/859 (§6-10); `application/vaccination/service/RescheduleVaccinationService.java:25-39`; `test/spec/UT-04_vaccination.md:26-27`.
- Missing `PATCH /api/v1/vaccinations/{vaccinationId}/reschedule` -> `404 VACCINATION-001`. Existing missing-ID tests invoke complete/cancel/delete, not reschedule.
- Proposed `UT-04-015`: nonexistent/deleted vaccination ID with valid scheduledDate -> 404/VACCINATION-001, no writes.
- Missing service branch: reschedule repository update returns false **after a valid CANCELLED row was read**. Existing UT-04-011 and section 2 #5 reject the initial state; they do not exercise this branch.
- Proposed JUnit `reschedule_vaccination_fail_when_transition_lost`: valid owner/CANCELLED row, repository `reschedule=false` -> VACCINATION-003, no successful transition. Distinct from the complete/cancel lost-update tests (#23/#24).

### G-05 [P2] Two reservation operations lack their own not-found error tests

- File/line: docs/07:1512 (§9-4), :1628 (§9-7); `CancelReservationService.java:20-21`, `CompleteReservationService.java:25-26` under `application/reservation/service/`.
- Missing endpoints/code: `PATCH /api/v1/reservations/{reservationId}/cancel` and `PATCH /api/v1/admin/reservations/{reservationId}/complete` -> `404 RESERVATION-006`.
- UT-09-010 tests GET; UT-10-007 tests approve; transition JUnit tests include missing approve/reject but no missing cancel/complete.
- Proposed `UT-09-013`, `UT-10-009`: nonexistent/deleted reservation ID; expect 404/RESERVATION-006 and no state write/notification. For admin complete, a valid admin and missing reservation must fail before affiliation lookup.

### G-06 [P2] User activation omits both per-API business errors

- File/line: docs/07:1921-1936 (shared §11-4..6 errors); `test/spec/UT-12_user-admin.md:19-21`; `AdminUserServiceTest.java:132-150`.
- Missing `PATCH /api/v1/admin/users/{userId}/activate` -> `400 USER-005` (self), `404 USER-003` (missing/deleted target).
- Existing self/not-found tests pass SUSPENDED, not ACTIVE; role has separate tests. The shared implementation is reassuring, but no item exercises the activation endpoint with either error.
- Proposed `UT-12-007`: self activation -> USER-005; missing/deleted target -> USER-003; no tokens or user fields change. A parameterized JUnit invocation with ACTIVE plus an endpoint-routing check also suffices.

### G-07 [P2] Fifteen role-protected endpoints have no denial test

- Files: `presentation/hospital/HospitalAdminController.java:68-98`, `HospitalBusinessHoursAdminController.java:27-55`, `HospitalScheduleAdminController.java:30-68`, `presentation/reservation/ReservationAdminController.java:43-55`, `presentation/user/AdminUserController.java:47-53` under the main Java package.
- Contract: docs/07 §2-4/§14 and explicit §11-9/11-11/11-4..6 `AUTH-005`. Testing one admin endpoint cannot detect a missing role annotation on another. Existing controller tests use disabled filters and do not supply these denial cases.
- Proposed parameterized HTTP item `UT-13-008`, one recorded subcase per route below. Use a valid ROLE_USER token and valid request/IDs: expect **403 AUTH-005**, no write/notification. GET must reveal no admin data. For system-admin-only affiliation endpoints, also test ROLE_HOSPITAL_ADMIN.

| Missing route (prefix `/api/v1`) | Closest existing item |
| --- | --- |
| PATCH `/admin/hospitals/{hospitalId}` | UT-05-007/008 (success/affiliation, not role denial) |
| GET `/admin/hospitals/{hospitalId}/admins` | UT-06-006 |
| DELETE `/admin/hospitals/{hospitalId}/admins/{userId}` | UT-06-007 |
| POST `/admin/hospitals/{hospitalId}/business-hours` | UT-07-001/008 |
| PATCH `/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | UT-07-005/006 |
| DELETE `/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | UT-07-007 |
| POST `/admin/hospitals/{hospitalId}/schedules` | UT-08-001/006 |
| POST `/admin/hospitals/{hospitalId}/schedules/generate` | UT-08-007/009 |
| PATCH `/admin/hospitals/{hospitalId}/schedules/{scheduleId}/block` | UT-08-004/006 |
| PATCH `/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock` | UT-08-004 |
| PATCH `/admin/reservations/{reservationId}/approve` | UT-10-003/007 |
| PATCH `/admin/reservations/{reservationId}/reject` | UT-10-004 |
| PATCH `/admin/reservations/{reservationId}/complete` | UT-10-005 |
| PATCH `/admin/users/{userId}/activate` | UT-12-003 |
| PATCH `/admin/users/{userId}/role` | UT-12-004/005 |

### G-08 [P2] Three affiliation-protected operations have no unmanaged-hospital test

- File/line: docs/07:141-153 (§2-8); `UpdateHospitalBusinessHoursService.java`, `DeleteHospitalBusinessHoursService.java`, `UnblockHospitalScheduleService.java` under `application/hospital/service/`, first `checkManageable` call in each `execute`.
- Missing `403 HOSPITAL-005`: PATCH and DELETE `/api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}`, PATCH `/api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock`.
- UT-07-008 covers create; UT-08-006 covers create/block. Mismatch-ID tests assert HOSPITAL-003/002 under authorized management, which is a different path. Generic `HospitalAccessChecker` tests do not prove these callers enforce it.
- Proposed `UT-07-009` (PATCH/DELETE), `UT-08-011` (unblock): real, unlinked hospital and valid child ID, ROLE_HOSPITAL_ADMIN -> 403/HOSPITAL-005; child unchanged. Repeat with nonexistent hospital to verify affiliation precedes child lookup.

### G-09 [P2] Business-hours update never rejects invalid hours

- File/line: docs/07:1256 (§8-9); `application/hospital/service/UpdateHospitalBusinessHoursService.java` validation call; `test/spec/UT-07_business-hours.md:21-22`.
- Missing endpoint/code: PATCH `/api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` -> `400 HOSPITAL-009`.
- UT-07-003 calls POST. Section 2 #18 tests the domain validator directly, not whether the PATCH path invokes it. No update service/controller test supplies invalid hours.
- Proposed `UT-07-010`: authorized PATCH with opening >= closing, slot length outside 5..240, or incomplete/out-of-hours break -> 400/HOSPITAL-009; existing hours unchanged.

### G-10 [P2] Remaining service error branches lack exercises

These are distinct branches in the current services, not a demand for 100% generated-code coverage. Shared helper branches already exercised by another operation are credited; separate service guards are not. Add parameterized JUnit cases where creating an inconsistent relation through real HTTP is impossible. All cases should assert the code and absence of writes/notifications.

| File/line under `spring-api/src/main/java/koh/portfolio/springapi/` | Endpoint / closest item | Missing condition -> expected result | Proposed item |
| --- | --- | --- | --- |
| `application/auth/service/RefreshTokenService.java:53-54` | POST `/api/v1/auth/refresh`; UT-01-010..012 | Stored valid refresh references a missing/deleted user -> AUTH-001 (401); no successor token | UT-01-017 / JUnit |
| `application/healthrecord/service/UpdateHealthRecordService.java:29` | PATCH `/api/v1/health-records/{healthRecordId}`; UT-03-008 | Record exists but its pet lookup is empty (e.g. logical deletion) -> HEALTH-002 (403) | UT-03-013 |
| `application/healthrecord/service/DeleteHealthRecordService.java:29` | DELETE same path; UT-03-011 | Same missing/deleted parent condition -> HEALTH-002 (403) | UT-03-014 |
| `application/vaccination/service/CompleteVaccinationService.java:29` | PATCH `/api/v1/vaccinations/{vaccinationId}/complete`; UT-04-012 | Existing vaccination, pet lookup empty -> VACCINATION-002 (403) | UT-04-016 |
| `application/vaccination/service/CancelVaccinationService.java:27` | PATCH `/api/v1/vaccinations/{vaccinationId}/cancel`; UT-04-008/009 | Same empty pet lookup -> VACCINATION-002 (403) | UT-04-017 |
| `application/vaccination/service/RescheduleVaccinationService.java:29` | PATCH `/api/v1/vaccinations/{vaccinationId}/reschedule`; UT-04-010/011 | Same empty pet lookup -> VACCINATION-002 (403) | UT-04-018 |
| `application/vaccination/service/DeleteVaccinationService.java:29` | DELETE `/api/v1/vaccinations/{vaccinationId}`; UT-04-013 | Same empty pet lookup -> VACCINATION-002 (403) | UT-04-019 |
| `application/reservation/service/CreateReservationService.java:39` | POST `/api/v1/reservations`; UT-09-006 | Schedule absent, rather than present under another hospital -> RESERVATION-002 (404) | UT-09-014 |
| `application/reservation/service/CreateReservationService.java:47` | POST same path; UT-09-007 | Schedule matches but hospital lookup empty, rather than SUSPENDED -> RESERVATION-009 (409) | UT-09-015 / JUnit |
| `application/reservation/service/GetReservationService.java:77/84/88` | GET `/api/v1/reservations` and `/{reservationId}`; UT-09-008/009 | Missing hospital or schedule -> RESERVATION-002 (404); pet missing even including deleted -> RESERVATION-004 (403). Exercise shared relation helpers with repository stubs; list and detail propagation can be parameterized | UT-09-016 / JUnit |

The persisted JaCoCo report independently confirms missed null-parent branches in the six record/vaccination mutation services and in reservation creation. UT-09 section 2 #13 already covers a logically deleted pet that remains available through `findByIdIncludingDeleted`; it does not cover an actually empty lookup. No duplicate gap is raised for that existing regression.

### G-11 [P2] Common SERVER-001 fallback has no test

- File/line: docs/07:98 (§2-4); `common/exception/GlobalExceptionHandler.java` `handleException`; `test/spec/UT-13_common.md:16-23`.
- Observed: UT-13 covers COMMON-001/002/003, AUTH-001, VALIDATION-001 and success, but no unhandled exception. No JUnit assertion for SERVER-001/500 exists; stored JaCoCo has 0 covered instructions in `handleException`.
- Proposed `UT-13-009` / MockMvc regression: make a use case throw an unexpected exception -> HTTP 500, `success=false`, `data=null`, `code=SERVER-001`, Japanese generic message, no internal exception details. Do not provoke a real production/scratch DB failure just to reach the handler.

### R-01 [P2] SQL evidence leaks a full Refresh Token

- File/line: `test/scripts/lib.mjs:73-75`, `test/scripts/api-cases.mjs` UT-01-010 (old-token SQL query); `test/evidence/api/UT-01-010.json`, DB step labeled `古いTokenの状態`, `query` property.
- Trigger: `CaseRun.db` records `query.trim()` without masking. The SQL embeds the full JWT in `WHERE token='...'` even though HTTP bodies mask token fields.
- Expected/observed: AC-05 says no secrets; the frozen evidence contains one full signed refresh token. It is revoked by this case, which limits immediate impact, but the writer also accepts arbitrary SQL/result strings and does not meet the stated masking contract.
- Correction: record a safe parameterized/redacted query or locate the token row by a nonsensitive identifier. Mask sensitive DB result/check/error strings where applicable; do not change the SQL actually executed. Regenerate the evidence and scan all evidence text for full JWTs and plaintext passwords. Do not copy the leaked value into a review or handoff.
- Disposition: open; high confidence, direct JSON and writer inspection.

### R-02 [P2] Reproduction instructions referenced by every spec do not exist

- File/line: `test/spec/UT-01_auth.md:7-9` (same generated lines in all 13 specs), `test/scripts/build-spec.mjs` generated environment/reviewer references; `test/scripts/run-api-unit-tests.mjs` setup.
- Observed: `test/README.md` is absent. Every spec delegates the environment to `README.md`; `../reviews/` also does not identify the task review record. Scripts assume an already-created scratch PostgreSQL database, container name, and running backend at port 18081. Their comments alone do not explain how to start that isolated backend or reset the database for a repeat run.
- Impact: AC-05 is not yet reproducible by a recipient. This is understandable for an in-progress package but must be completed before final evidence.
- Correction: add the promised README with working directories, JDK/Node/DB requirements, safe scratch-DB create/reset commands, backend datasource/port startup, API runner, JUnit/JaCoCo/spec-generation commands and evidence locations. Link the actual review record. Keep the preserved review-home directory. Validate the documented sequence on a clean scratch DB at the next implementer run.
- Disposition: open; high confidence. No requirement to finish excluded screen evidence in this round.

## Endpoint and per-API error inventory (request 1)

Derived from controller mapping annotations (52 routes) and docs/07 error tables, including shared §5-2/4/5, §11-4..6 and affiliation §2-8. Prefix all paths below with `/api/v1`. `Axx-nnn` means existing API item `UT-xx-nnn`; `Jxx#n` means section-2 JUnit row n in that feature spec. A code followed by a G ID is a gap, not existing coverage. These are **per-API** errors; shared authentication/validation/fallback policy is summarized after the table. AUTH-005 gaps on guarded routes are additionally listed in G-07.

| Controller (under `presentation/`) | Route | API item exercising route | Per-API errors -> evidence / gap |
| --- | --- | --- | --- |
| auth/AuthController | POST `/auth/login` | A01-005 | AUTH-001 A01-006/007; AUTH-002 A01-008 |
| auth/AuthController | POST `/auth/refresh` | A01-010 | AUTH-003 A01-010/011; AUTH-004 J01#10; AUTH-002 A01-012; additional AUTH-001 branch G-10 |
| auth/AuthController | POST `/auth/logout` | A01-013 | AUTH-001 A01-016 |
| user/UserController | POST `/users` | A01-001 | USER-001 A01-002; VALIDATION-001 A01-003/004 |
| user/UserController | GET `/users/me` | A01-009 | No separate per-API table; AUTH-001 A01-014/015, USER-003 service branch J01#2 |
| pet/PetController | GET `/pets` | A02-006 | No separate per-API table |
| pet/PetController | GET `/pets/{petId}` | A02-007 | PET-001 A02-009; PET-002 A02-008 |
| pet/PetController | POST `/pets` | A02-001/002 | No separate table; VALIDATION-001 A02-003..005 |
| pet/PetController | PATCH `/pets/{petId}` | A02-010 | PET-001 J02#10/#15; PET-002 A02-011 |
| pet/PetController | DELETE `/pets/{petId}` | A02-012 | PET-001 J02#11; PET-002 A02-013 |
| healthrecord/HealthRecordController | GET `/pets/{petId}/health-records` | A03-005 | HEALTH-002 A03-006 |
| healthrecord/HealthRecordController | POST `/pets/{petId}/health-records` | A03-001/002 | HEALTH-002 A03-004/J03#10; VALIDATION-001 A03-003 |
| healthrecord/HealthRecordController | PATCH `/health-records/{healthRecordId}` | A03-007 | HEALTH-001 A03-009/J03#15; HEALTH-002 A03-008 (null parent G-10); VALIDATION-001 G-03 |
| healthrecord/HealthRecordController | DELETE `/health-records/{healthRecordId}` | A03-010 | HEALTH-001 A03-010 repeat; HEALTH-002 A03-011 (null parent G-10) |
| vaccination/VaccinationController | GET `/pets/{petId}/vaccinations` | A04-004 | VACCINATION-002 J04#12 (shared ownership helper also exercised by A04-003) |
| vaccination/VaccinationController | POST `/pets/{petId}/vaccinations` | A04-001 | VACCINATION-002 A04-003; VALIDATION-001 A04-002 |
| vaccination/VaccinationController | PATCH `/vaccinations/{vaccinationId}/complete` | A04-005 | VACCINATION-001 J04#13; -002 A04-012 (null parent G-10); -003 A04-007/J04#23; VALIDATION-001 A04-006 |
| vaccination/VaccinationController | PATCH `/vaccinations/{vaccinationId}/cancel` | A04-008 | VACCINATION-001 J04#14; -002 J04#18 (null parent G-10); -003 A04-009/J04#24 |
| vaccination/VaccinationController | PATCH `/vaccinations/{vaccinationId}/reschedule` | A04-010 | VACCINATION-001 G-04; -002 J04#15 (null parent G-10); -003 A04-011 (lost update G-04); VALIDATION-001 J04#45 |
| vaccination/VaccinationController | DELETE `/vaccinations/{vaccinationId}` | A04-013 | VACCINATION-001 A04-013 repeat/J04#22; -002 J04#17/#37 (null parent G-10) |
| hospital/HospitalController | GET `/hospitals` | A05-004 | No separate per-API table |
| hospital/HospitalController | GET `/hospitals/{hospitalId}` | A05-005 | No separate table; HOSPITAL-001 A05-006 |
| hospital/HospitalScheduleController | GET `/hospitals/{hospitalId}/schedules` | A08-003 | No separate table; HOSPITAL-001 J08#6 |
| hospital/HospitalScheduleAdminController | POST `/admin/hospitals/{hospitalId}/schedules` | A08-001 | HOSPITAL-005 A08-006; HOSPITAL-001 A08-002; VALIDATION-001 A08-002 |
| hospital/HospitalScheduleAdminController | PATCH `/admin/hospitals/{hospitalId}/schedules/{scheduleId}/block` | A08-004 | HOSPITAL-002 A08-005/J08#9; HOSPITAL-005 A08-006 |
| hospital/HospitalScheduleAdminController | PATCH `/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock` | A08-004 | HOSPITAL-002 J08#8/#10; HOSPITAL-005 G-08 |
| hospital/HospitalScheduleAdminController | POST `/admin/hospitals/{hospitalId}/schedules/generate` | A08-007/008 | VALIDATION-001 A08-009/J08#24; HOSPITAL-010 A08-009; HOSPITAL-005 A08-009; HOSPITAL-001 J08#1 (real generator guard) |
| hospital/HospitalBusinessHoursController | GET `/hospitals/{hospitalId}/business-hours` | A07-004 | HOSPITAL-001 A07-004 |
| hospital/HospitalBusinessHoursAdminController | POST `/admin/hospitals/{hospitalId}/business-hours` | A07-001 | HOSPITAL-001 J07#8; HOSPITAL-004 A07-002; HOSPITAL-009 A07-003; HOSPITAL-005 A07-008 |
| hospital/HospitalBusinessHoursAdminController | PATCH `/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | A07-005 | HOSPITAL-003 A07-006/J07#5/#11; HOSPITAL-009 G-09; HOSPITAL-005 G-08 |
| hospital/HospitalBusinessHoursAdminController | DELETE `/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | A07-007 | HOSPITAL-003 J07#6/#10; HOSPITAL-005 G-08 |
| reservation/ReservationController | GET `/reservations` | A09-008 | No separate table; relation lookup errors G-10 |
| reservation/ReservationController | GET `/reservations/{reservationId}` | A09-009 | RESERVATION-006/-007 A09-010; relation lookup errors G-10 |
| reservation/ReservationController | POST `/reservations` | A09-001 | RESERVATION-001 A09-002; -002 A09-006 (absent schedule G-10); -003 A09-003; -004 A09-004/J09#7; -005 A09-005; -009 A09-007 (absent hospital G-10) |
| reservation/ReservationController | PATCH `/reservations/{reservationId}/cancel` | A09-011 | RESERVATION-006 G-05; -007/-008 A09-012, lost update J10#17 |
| reservation/ReservationAdminController | GET `/admin/reservations` | A10-001/002 | VALIDATION-001 A10-008/J10#26; HOSPITAL-005 A10-008; AUTH-005 A10-008 |
| reservation/ReservationAdminController | PATCH `/admin/reservations/{reservationId}/approve` | A10-003 | RESERVATION-006 A10-007; -008 A10-006/J10#20; HOSPITAL-005 A10-007/J10#18 |
| reservation/ReservationAdminController | PATCH `/admin/reservations/{reservationId}/reject` | A10-004 | RESERVATION-006 J10#22; -008 J10#3..6/#19; HOSPITAL-005 J10#18 |
| reservation/ReservationAdminController | PATCH `/admin/reservations/{reservationId}/complete` | A10-005 | RESERVATION-006 G-05; -008 A10-006/J10#21; HOSPITAL-005 J10#18 |
| notification/NotificationController | GET `/notifications` | A11-001 | No separate per-API table |
| notification/NotificationController | PATCH `/notifications/{notificationId}/read` | A11-002 | NOTIFICATION-001/-002 A11-003 |
| hospital/HospitalAdminController | POST `/admin/hospitals` | A05-001 | No separate table; VALIDATION-001 A05-002; AUTH-005 A05-003 |
| hospital/HospitalAdminController | PATCH `/admin/hospitals/{hospitalId}` | A05-007 | HOSPITAL-001 J05#3/#5; HOSPITAL-005 A05-008 |
| hospital/HospitalAdminController | PATCH `/admin/hospitals/{hospitalId}/suspend` | A05-009 | HOSPITAL-001/AUTH-005 A05-010 |
| hospital/HospitalAdminController | GET `/admin/hospitals/mine` | A05-011 | No separate table; AUTH-005 A12-004 (before role change) |
| hospital/HospitalAdminController | GET `/admin/hospitals/{hospitalId}/admins` | A06-006 | HOSPITAL-001 A06-006; AUTH-005 G-07 |
| hospital/HospitalAdminController | POST `/admin/hospitals/{hospitalId}/admins` | A06-001 | VALIDATION-001/HOSPITAL-001/USER-003 A06-004; HOSPITAL-006 A06-003; HOSPITAL-007 A06-002; AUTH-005 A06-005 |
| hospital/HospitalAdminController | DELETE `/admin/hospitals/{hospitalId}/admins/{userId}` | A06-007 | HOSPITAL-008 A06-007 repeat; AUTH-005 G-07 |
| user/AdminUserController | GET `/admin/users` | A12-001 | No separate table; AUTH-005 A12-006; VALIDATION-001 J12#16 |
| user/AdminUserController | PATCH `/admin/users/{userId}/suspend` | A12-002 | USER-003/USER-005 A12-005; AUTH-005 A12-006 |
| user/AdminUserController | PATCH `/admin/users/{userId}/activate` | A12-003 | USER-003/USER-005 G-06; AUTH-005 G-07 |
| user/AdminUserController | PATCH `/admin/users/{userId}/role` | A12-004/A06-008 | USER-003/USER-005 J12#3; AUTH-005 G-07; VALIDATION-001 A12-005/J12#17 |

Scheduler inventory: `HospitalScheduleGenerationScheduler.generateNextMonth` -> G-01; `VaccinationReminderScheduler.remind` -> G-02. Their downstream services are covered; the scheduler entry methods are not.

Common errors: AUTH-001 is exercised by A01-014..016/A13-001 and JWT JUnit tests; VALIDATION-001 by A13-002/003 plus domain request cases; COMMON-001/002/003 by A13-004/005/007 and J05#28/#29/J02#24. AUTH-005 has existing real HTTP evidence but fifteen route-specific omissions (G-07). SERVER-001 is absent (G-11). Common parser/unauthenticated cases are credited as representative middleware tests, not duplicated as 52 independent gaps; specific documented domain/authorization branches are assessed separately above.

The Django analysis endpoint in docs/07 §7 is explicitly unimplemented and has no Spring controller. It is excluded, not silently counted among the 52 Spring endpoints.

## Expected results and defect dispositions (requests 2 and 4)

- No contradictory status/error expectation was found among the inspected frozen API definitions, actual recorded responses, and current docs/07 tables. In particular, **PET-002 is 401**, AUTH-002 is 403, reservation completion produces **TREATMENT_COMPLETED**, vaccination reschedule requires CANCELLED, and break slots count toward generated slots. These unusual details were not treated as defects merely for being unusual.
- Weak checks to strengthen with the affected items: UT-13-004/005/007 assert HTTP status only; add COMMON-001/002/003 and the error envelope to the HTTP checks (JUnit already checks those codes, so these are not additional uncovered-code gaps). UT-10-004 checks `min(notification_type)` without count/recipient; UT-10-005 checks notification types without recipient. Assert exactly one new notification for the reservation's user. These checks should fail on duplicate/wrong-recipient notifications, as the stated expectations require.
- D-001: sound **documentation correction**. Bean validation is mapped to VALIDATION-001; USER-002 is not thrown. A01-003/004 exercise the corrected contract.
- D-002: current **documentation correction** is sound. AuthController uses the authenticated Long principal and LogoutService revokes by user; the response is `isLogout=true`. A01-013/016 match that contract and J01#8/#9 cover revoke/locking. The claimed historical date/cause of the old wording was not independently re-audited outside the allowed files.
- D-003: sound **code fix**. The explicit NoResourceFoundException handler prevents the generic Exception handler from turning a missing resource into 500; J05#29 and A13-004 agree.
- D-004: sound **code fix** for the observed 500-to-405 problem; J05#28 and A13-005 agree.
- D-005: sound **code fix** for the observed 500-to-415 problem; J02#24 checks COMMON-003 and no registration interaction; A13-007 records 415.
- D-006: sound **enum documentation correction**, not removal of BIRD support. The final case uses DRAGON and obtains VALIDATION-001. No expansion of product scope is needed.

## Verification

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
| --- | --- | --- | --- | --- |
| Frozen target | Repository root: parse each `hash *path` entry and compare `Get-FileHash -Algorithm SHA256 -LiteralPath path`; `git branch --show-current`, `git rev-parse HEAD`, `git status --short` | Initially 211 entries, 0 mismatches; correct branch/HEAD. Final recheck: only `test/DEFECTS.md` changed (new D-007 for excluded screens); D-001..006 and all other 210 manifest entries unchanged. Unrelated changes were not reviewed or overwritten | None | Next handoff must freeze and hash the updated snapshot |
| AC-01 | `rg` controller mappings, docs/07 headings/error rows, API metadata/recorded HTTP steps, section-2 JUnit names; inspect only relevant service/test branches; inspect stored JaCoCo method/line counters | 52/52 endpoints have API items; full inventory above | None needed for static inventory | G-01..G-11 |
| AC-02 | Compare API definitions, evidence status/code, current docs/07 and changed handler/controller regression tests | D-001..006 dispositions supported; no contradictory expectation found in inspected cases | Initial 112/123 run/history not reproduced | Strengthen noted assertions |
| AC-03 JUnit rerun | `E:/workspace/daipetto/spring-api`: PowerShell `$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle/review-home'; ./gradlew.bat test --offline` | Exit 0, but all 6 tasks UP-TO-DATE; not credited as a fresh test execution | Stored 321 tests | Follow-up below |
| AC-03 fresh JUnit | Same directory/env: `./gradlew.bat test --offline --rerun-tasks` | **Exit 0; 6 tasks executed; BUILD SUCCESSFUL, 1m 4s. Fresh XML: 321 tests, 0 failures/errors, 0 skipped** | Stored XML also 321/0/0 | No PostgreSQL claim |
| AC-03 evidence consistency | Node read-only JSON checks; count spec section-2 rows and `<testcase>` elements; compare stored/fresh testcase identities | 125 API JSONs, all verdict OK and nonempty/all-true checks; 321 spec rows = 321 stored XML tests; fresh suite count matches | HTTP/DB response authenticity and 125/125 PostgreSQL run are implementer-reported | Missing cases still required |
| AC-05 masking | Inspect HTTP and DB steps; scan raw JSON for complete JWT shape and inspect password fields | HTTP token/password fields sampled are masked; **one full JWT in UT-01-010 SQL** | None | R-01 |
| AC-05 reproducibility | Inspect runner/lib/build-spec and referenced local paths | Scripts present; README and referenced review location absent | Scratch DB/backend setup/run reported | R-02 |
| AC-04 | Excluded screen round | NOT RUN | Screen work in progress | Outside this review |

JUnit environment: Windows PowerShell, OpenJDK 17.0.19 Microsoft, Gradle wrapper 8.14.4, Node v24.17.0. Nonfatal MapStruct unmapped-property and Windows Java preferences warnings appeared; they did not fail the run. Published `test/evidence/` files were not overwritten by the reviewer; fresh results remain under `spring-api/build/`. The requested `.gradle/review-home` was preserved.

Per-feature published JUnit section-2 counts, independently counted: UT-01 40, UT-02 29, UT-03 25, UT-04 47, UT-05 29, UT-06 8, UT-07 29, UT-08 27, UT-09 24, UT-10 30, UT-11 12, UT-12 20, UT-13 1 = **321**. API evidence metadata and the corresponding 125 spec rows were checked structurally; all metadata matches the definitions. Stored and fresh testcase identities match, not merely the totals.

## Remaining risks and next action

Claude Code should add the missing items/JUnit cases, fix SQL evidence redaction, finish reproduction instructions, strengthen the noted HTTP/notification assertions, rerun against a fresh scratch PostgreSQL database, regenerate specs/evidence, and publish HANDOFF-002 with a new hash manifest. Keep this review and HANDOFF-001 unchanged. Coordinate the next freeze with the screen work: the final hash mismatch shows the shared DEFECTS file was still being edited during this review. The new D-007 and concurrent frontend changes are outside this round and receive no acceptance here.

No Docker/PostgreSQL run was attempted by this reviewer, as allowed by this round. The published API/DB evidence is **reported-only**; H2/mocks and this fresh JUnit run do not prove PostgreSQL constraints or concurrency. Screen/Capacitor behavior is not reviewed here. Only this reviewer-owned record was authored; application/test/docs files were not edited.
