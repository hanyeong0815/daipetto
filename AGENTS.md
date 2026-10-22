# Daipetto — Codex Context

Pet health management + hospital reservation platform. Portfolio for a backend job search in Japan — document/implementation consistency is itself a showcased skill, so keep docs in sync (§11).

This is the repository entry point for Codex and the canonical shared project policy for both agents. Do not duplicate sections 1–12 in CLAUDE.md. Historical handoff files are not startup instructions.

## Agent startup and collaboration

- Both agents: read [.ai-collab/PROTOCOL.md](.ai-collab/PROTOCOL.md), then only the task directory selected by the user. Treat dated status below as a snapshot; verify the actual branch, HEAD, files, and tests.
- Codex only: read [.codex/ROLE.md](.codex/ROLE.md). For reviews also read [.codex/REVIEW.md](.codex/REVIEW.md).
- Claude Code: follow the role instructions in [CLAUDE.md](CLAUDE.md); do not inherit Codex's reviewer role just because this shared file is being read.
- Agent-facing files in `.codex/` and `.ai-collab/` use concise English. User-facing replies remain Korean; product design docs and code comments retain the language rules below.
- Operational source of truth: `.ai-collab/PROTOCOL.md`. The original research corrections remain in `docs/codex-workflow/01_CORRECTIONS.md` for reference only.

Last updated: 2026-10-23 (branch `test/unit-test`, from `develop`)

## 1. Areas

| Dir | Stack | IDE |
|---|---|---|
| `frontend/` | React 18 / TypeScript / Vite / Tailwind / Zustand / Axios / React Router 6 / Capacitor (Android·iOS) | VSCode |
| `spring-api/` | Java 17 / Spring Boot 3.3.13 / Spring Security / JPA / Flyway / MapStruct | IntelliJ |
| `django-api/` | Python 3.12.10 / DRF 3.15.2 — **not started** (only a Dockerfile exists) | VSCode |
| `docs/` | 13 design docs, Japanese, change-managed (§11, §12) | — |

## 2. Language rules

| Target | Language |
|---|---|
| Replies to the user | Korean |
| docs/, code comments (keep minimal) | Japanese |
| Code identifiers, test method names | English |
| `@DisplayName`, API error messages | Japanese |

## 3. Spring architecture (fixed — do not change)

```
Controller → UseCase interface → UseCase Service → Domain model
→ Repository Port → Persistence Adapter → MapStruct Mapper ↔ JPA Entity
→ JpaRepository → PostgreSQL
```

Packages under `spring-api/src/main/java/koh/portfolio/springapi/`:

```
common/        exception (ErrorCode, CustomException, Preconditions, GlobalExceptionHandler),
               response (ApiResponse), time (ServerTime)
domain/{X}/    model / exception / port
application/{X}/ dto / usecase / service
infrastructure/  mapper (DomainEntityMapper), persistence/{X}, security (jwt, handler, cors)
presentation/{X}/ controllers
```

Rules:
1. Domain never depends on JPA. Domain ↔ Entity conversion only via MapStruct (`DomainEntityMapper<DOMAIN, ENTITY>`).
2. UseCase = interface, Service = implementation. Simple domains (User, Pet): one Service may implement several UseCases. Complex domains (Reservation): one Service per UseCase.
3. Business errors: domain `ErrorCode` enum (separate `code()`, not `name()`) + `CustomException` + `Preconditions.validate(...)`. All responses via `ApiResponse`. `GlobalExceptionHandler` handles CustomException / AccessDeniedException / MethodArgumentNotValidException / Exception.
4. `userId` comes from `Authentication.getPrincipal()` as `Long` — never in request DTOs.
5. Prefer explicit `@Modifying` update queries over dirty checking for important state changes. **Never persist an update by `save()`-ing a previously read entity**: write only the columns the operation owns, put its precondition in the `WHERE` (`deleted_at IS NULL`, expected status, owning parent id), and treat 0 affected rows as not-found/conflict. A full save rewrites `status`/`deleted_at` with stale values and silently undoes concurrent suspensions and deletions (docs/12 §3-12). `save()` is for inserts only. When a partial update fills unsent fields from the read row, read it with `@Lock(PESSIMISTIC_WRITE)` (`findByIdForUpdate`) so a concurrent commit to an unsent column is not written back.
6. Role checks via `@PreAuthorize("hasAuthority('ROLE_...')")`; SecurityConfig only guarantees `authenticated()`.
7. Owner checks with the `Preconditions.validate` pattern (see `UpdatePetService`).
8. Domain state transitions are methods returning a new immutable instance (see `Hospital.suspend()`, `Reservation.approve()`); Lombok on Domain: `@Getter`/constructors only, never `@Setter`/`@Data`.
9. Auth policy: access token 30 min / refresh token 14 days, refresh **rotation** (old token revoked on every use), max 1 active refresh token per user, stored in PostgreSQL (no Redis in MVP). Login, refresh and logout all take `SELECT ... FOR UPDATE` on the user row (`UserRepository.lockForSessionUpdate`) before touching tokens — without that per-user serialization the bulk revoke misses a successor token inserted by a concurrent refresh (docs/12 §3-11). `JwtAuthenticationFilter` loads the user on every authenticated request: suspended, deleted or missing users are not authenticated (401) and authorities come from the current DB role, so admin suspension and role changes apply on the next request (docs/09 §3-5). Admin suspension also takes the session lock and revokes all refresh tokens.

## 4. API common spec

Success: `{"success": true, "data": {}}`
Error: `{"success": false, "data": null, "code": "AUTH-001", "message": "認証に失敗しました。"}`
Error messages in Japanese. New codes follow `{DOMAIN}-{seq}`; `{DOMAIN}-999` is the domain default.

## 5. ErrorCodes (implemented)

| Code | Meaning | HTTP |
|---|---|---|
| AUTH-001 | 認証失敗 | 401 |
| AUTH-002 | 利用停止中アカウント | 403 |
| AUTH-003 | 無効なRefresh Token | 401 |
| AUTH-004 | 期限切れRefresh Token | 401 |
| AUTH-005 | アクセス権限なし | 403 |
| AUTH-006 | 非サポートToken | 401 |
| USER-001 | 重複メール | 409 |
| USER-002 | 入力バリデーション（定義のみ・未使用。会員登録の入力エラーもVALIDATION-001） | 400 |
| USER-003 | 存在しないユーザー | 404 |
| USER-004 | 利用停止中ユーザー | 403 |
| USER-005 | 自分自身の停止・停止解除・権限変更 | 400 |
| PET-001 | 存在しないペット | 404 |
| PET-002 | 飼い主不一致 | 401 |
| HOSPITAL-001 | 存在しない病院 | 404 |
| HOSPITAL-002 | 存在しない予約枠 / hospitalId不一致 | 404 |
| HOSPITAL-003 | 存在しない営業時間 / hospitalId不一致 | 404 |
| HOSPITAL-004 | 曜日重複登録 | 409 |
| HOSPITAL-005 | 管理対象外の病院（病院管理者が未紐付け） | 403 |
| HOSPITAL-006 | 病院管理者権限でないユーザーの紐付け | 400 |
| HOSPITAL-007 | 紐付けの重複 | 409 |
| HOSPITAL-008 | 存在しない紐付けの解除 | 404 |
| HOSPITAL-009 | 不正な営業時間（開始>=終了、単位5〜240分外、休憩の片方のみ・営業時間外） | 400 |
| HOSPITAL-010 | 予約枠を生成できない月（過去・13か月以上先） | 400 |
| RESERVATION-001 | 重複予約（同一枠にREQUESTED/APPROVED既存） | 409 |
| RESERVATION-002 | 存在しない予約枠 / hospitalId不一致 | 404 |
| RESERVATION-003 | BLOCKED枠への予約 | 409 |
| RESERVATION-004 | 存在しない/他人のペット | 403 |
| RESERVATION-005 | 過去日時の枠 | 400 |
| RESERVATION-006 | 存在しない予約 | 404 |
| RESERVATION-007 | 予約者本人でない | 403 |
| RESERVATION-008 | 不正な状態遷移（同時実行で負けた側を含む） | 409 |
| RESERVATION-009 | SUSPENDED病院への予約 | 409 |
| HEALTH-001 | 存在しない健康記録 | 404 |
| HEALTH-002 | 存在しないペット / 他ユーザーのペット | 403 |
| NOTIFICATION-001 | 存在しない通知 | 404 |
| NOTIFICATION-002 | 受信者本人でない | 403 |
| VACCINATION-001 | 存在しないワクチン接種予定 | 404 |
| VACCINATION-002 | 存在しないペット / 他ユーザーのペット | 403 |
| VACCINATION-003 | 不正な状態遷移（同時実行で負けた側を含む） | 409 |
| AUTH/USER/PET/HOSPITAL/RESERVATION/HEALTH/NOTIFICATION/VACCINATION-999 | domain default | 500 |
| VALIDATION-001 | `@Valid` failure, unreadable JSON, bad query/path value, missing required param (GlobalExceptionHandler) | 400 |
| COMMON-001 | 存在しないURL | 404 |
| COMMON-002 | 許可されていないHTTPメソッド | 405 |
| COMMON-003 | 対応していないContent-Type | 415 |
| SERVER-001 | unhandled exception fallback | 500 |

## 6. Enums / state transitions

| Target | Values |
|---|---|
| Role | ROLE_USER / ROLE_HOSPITAL_ADMIN / ROLE_SYSTEM_ADMIN |
| users.status, hospitals.status | ACTIVE / SUSPENDED |
| hospital_schedules.status | AVAILABLE / BLOCKED |
| reservations.status | REQUESTED / APPROVED / REJECTED / COMPLETED / CANCELLED |
| vaccinations.status | SCHEDULED / COMPLETED / CANCELLED |
| NotificationType | RESERVATION_APPROVED / RESERVATION_REJECTED / VACCINATION / TREATMENT_COMPLETED |

Reservation transitions (only these; enforced in the Domain model):

| From | To | Actor |
|---|---|---|
| REQUESTED | APPROVED / REJECTED | HOSPITAL_ADMIN |
| REQUESTED | CANCELLED | USER |
| APPROVED | COMPLETED | HOSPITAL_ADMIN |
| APPROVED | CANCELLED | USER |

Forbidden: COMPLETED→REQUESTED, CANCELLED→APPROVED, REJECTED→APPROVED, COMPLETED→CANCELLED. Max 1 REQUESTED/APPROVED per schedule_id. Approve/reject/complete create a Notification in the same transaction, only when the transition succeeds (docs/08 §6-6, docs/07 §10).
`hospital_schedules.status` only expresses hospital-side availability; "already booked" is judged from `reservations` (no auto-BLOCK on booking — docs/06 §18).

## 7. DB / Flyway

- DB changes only via Flyway; logical delete (`deleted_at`) is the default.
- Applied on this branch: V1 users, V2 refresh_tokens, V3 pets, V4 hospitals, V5 hospital_business_hours, V6 hospital_schedules, V7 reservations, V8 partial unique index `uq_reservations_active_schedule` (at most one REQUESTED/APPROVED reservation per schedule — PostgreSQL partial index, not replayed by the H2 test profile), V9 health_records, V10 notifications, V11 vaccinations (+ FK `notifications.vaccination_id`), V12 `vaccinations.reminded_date` (reminder dedupe marker), V13 pets breed/neutered/microchip_number (the former pet-fields V4, renumbered at merge), V14 `pets.gender` nullable (matches docs/06), V15 hospital_admins (user↔hospital links, unique per pair).
- New migrations start at **V16**; check every branch for number collisions before numbering.
- ⚠ V8 fails on an existing database that already holds duplicate active reservations for one schedule; clean those rows before applying.
- Hospital affiliation: services that manage a hospital call `HospitalAccessChecker.checkManageable(userId, hospitalId)` first (SYSTEM_ADMIN: any hospital; HOSPITAL_ADMIN: only hospitals linked in `hospital_admins`; else HOSPITAL-005). Reservation transitions check the reservation's hospital. Docs: docs/07 §2-8, docs/09 §4-5.

## 8. Test rules

- Method names English, `@DisplayName` Japanese, comments minimal.
- Spring Boot 3.3.13 → use `@MockBean` (never `@MockitoBean`).
- Controller tests: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` + `@Import(GlobalExceptionHandler.class)`. If `JwtAuthenticationFilter` breaks context creation, exclude it via `excludeFilters`. Mock **every** UseCase the controller depends on.
- Principal is `Long`: use `new UsernamePasswordAuthenticationToken(1L, null, authorities)` — `.with(user(...))` fails.
- Most unit tests mock repositories. H2 **is** available for `@DataJpaTest` (`application-test.yml`, `MODE=PostgreSQL`, `ddl-auto=create-drop`, Flyway disabled) — see `RefreshTokenPersistenceAdapterTest`; Testcontainers is not introduced.
- Mocked repositories prove neither real SQL nor concurrency. Null-tolerant `@Query` bugs escape them (docs/12 §3-10) and so do races (docs/12 §3-11). `./gradlew test` has no PostgreSQL concurrency test (H2/mocks only). Manual PostgreSQL race checks exist as task scripts run against a scratch DB: refresh/login vs suspension (`.ai-collab/tasks/2026-10-21-user-management/scripts/`) and concurrent/racing hospital-admin links (`.ai-collab/tasks/2026-10-22-hospital-admins/scripts/` HA-12/HA-13). Their results are implementer-reported unless a reviewer reruns them.
- Verify: `./gradlew clean test` in `spring-api/` (342 green as of 2026-10-23). `test` is finalized by JaCoCo (`build/reports/jacoco/test/`, XML + HTML).
- Full unit-test package (単体テスト): `test/` at the repository root — `test/README.md` (plan, environment, how to rerun), `test/spec/UT-01..UT-14` (items, expected results, results, evidence links), `test/evidence/` (JUnit XML, JaCoCo, per-item API JSON from a scratch PostgreSQL run, screen captures), `test/DEFECTS.md`. Regenerate specs with `node test/scripts/build-spec.mjs`; API items live in `test/scripts/api-cases.mjs` (expected results from docs/07).

## 9. Environment

| Item | Value |
|---|---|
| Spring | Java 17 / Boot 3.3.13 / Gradle 8.14.4 / Flyway 10.20.x / MapStruct 1.5.5.Final / jjwt 0.12.6 |
| Frontend | React 18.3.1 / TS 5.7.x / Vite 6.4.x / Node 24.17.0 / Tailwind 3.4.x / Zustand 5 / Axios 1.18 / Router 6.30 / Capacitor 8.4 |
| Django | Python 3.12.10 / DRF 3.15.2 (not started) |
| DB | PostgreSQL 16.9 (db/user: daipetto) |
| Ports | frontend 5173 / spring 8080 / django 8000 / postgres 5432 |
| Timezone | JST `Asia/Tokyo` everywhere (Japan-facing service). Spring sets the JVM default in `main` from `ServerTime.ZONE_ID`; postgres container `TZ=Asia/Tokyo`; frontend computes "today" with `todayInJapan()` (`src/utils/date.ts`), never `toISOString()`; Django `TIME_ZONE` must match when built. |

Start: `docker compose up -d` (postgres) → `./gradlew bootRun` / `npm run dev`.
API base URL is resolved at runtime per platform via `Capacitor.getPlatform()` (`VITE_API_BASE_URL_WEB` / `_ANDROID` / `_IOS`).

## 10. Status (2026-10-22)

**Vaccination (2026-10-05, branch `feat/spring/vaccination`, stacked on `feat/react-api-integration`)**: register/list/complete/cancel/reschedule/delete APIs + daily 09:00 JST reminder Scheduler creating VACCINATION notifications (dedupe: `reminded_date` claimed by a conditional UPDATE per scheduled date, cleared on reschedule). Task record `.ai-collab/tasks/2026-10-05-vaccination/`; Codex REVIEW-001 found R-01..R-04 (stale pet rows, failed load shown as empty, rescheduled reminder lost to a timestamp race, copy); fixed in HANDOFF-002 and ACCEPTED in REVIEW-002 (2026-10-21). Frontend: SC-016 vaccination screen and the SC-006 upcoming-vaccination panel (2026-10-21).

**Pet fields + datetime format (2026-10-21, branch `feat/spring/pet-fields-datetime`)**: merged the July pet-fields branch (breed/neutered/microchip, V13) and added those columns to the conditional `updateProfile`; pet update stays a full replacement (user decision, docs/07 §5-4); `pets.gender` made nullable (V14) and request validation added for name/breed/weight lengths; all response `LocalDateTime`s serialize as JST with offset (`LocalDateTimeJsonSerializer`, docs/07 §2-7, user decision). Frontend: pet edit screen (`/pets/:petId/edit`, reuses SC-005), breed on the pet list, 「不明」 gender. Task record `.ai-collab/tasks/2026-10-21-pet-fields-datetime/`; Codex REVIEW-001 ACCEPTED (2026-10-21, one P3 comment fix deferred).
**User management (2026-10-21, branch `feat/spring/user-management`)**: SYSTEM_ADMIN APIs `GET /admin/users` (keyword/role/status, paged `PageResponse`), `PATCH /admin/users/{id}/suspend|activate|role` (self → USER-005; suspension locks the user row and revokes refresh tokens). The JWT filter now checks the current user status/role per request. `GlobalExceptionHandler` maps unreadable params/JSON (bad enum values) to 400 VALIDATION-001 instead of 500. Frontend: real SC-015 `AdminUserPage`, a role-dependent admin menu on the dashboard (admin screens had no entry point), `UserProfile.userId` fixed (the type said `id`). Task record `.ai-collab/tasks/2026-10-21-user-management/`; REVIEW-001 found R-01 (login overlapping suspension could issue a token), R-02 (admin list stranded on an empty page), R-03 (status lines); fixed in HANDOFF-002 and ACCEPTED in REVIEW-002 (2026-10-22).
**Hospital affiliation (2026-10-22, branch `feat/spring/hospital-admins`)**: V15 `hospital_admins`; SYSTEM_ADMIN links/unlinks HOSPITAL_ADMIN users (`GET/POST/DELETE /admin/hospitals/{id}/admins`; assignment takes the user row lock so a concurrent demotion cannot leave a link); demotion away from HOSPITAL_ADMIN deletes all links. Every hospital-managing service checks affiliation first (HOSPITAL-005..008). New `GET /admin/hospitals/mine` and `GET /admin/reservations?hospitalId=&status=&date=`; a missing required query param is 400 instead of 500. Frontend: SC-013 `AdminReservationPage` on the real API (was mock data), SC-014 lists managed hospitals (suspended included) with a SYSTEM_ADMIN hospital-admin section. Task record `.ai-collab/tasks/2026-10-22-hospital-admins/`; REVIEW-001 found R-01 (a late refresh after suspend/save could swap the selected hospital's data), R-02 (failed list load shown as "no affiliation"), R-03 (AGENTS wording); fixed in HANDOFF-002 and ACCEPTED in REVIEW-002 (2026-10-22).
**Slot auto-generation (2026-10-22, branch `feat/spring/schedule-generation`)**: `HospitalScheduleGenerationScheduler` (1st of each month 00:00 JST) generates next month's `hospital_schedules` for every ACTIVE hospital from `hospital_business_hours`, one transaction per hospital (`HospitalScheduleGenerator`, failures logged and skipped). `POST /admin/hospitals/{id}/schedules/generate {month}` does the same for this month..+12 (affiliation-checked, HOSPITAL-010 otherwise; this month from today). Segments are split by the slot length, break slots are BLOCKED, a day with any existing slot is skipped, and the hospital row lock serializes generations. Business hours are now validated on create/update (HOSPITAL-009; previously 0-minute slots or reversed times were accepted, docs/12 §3-18). Frontend: SC-014 「予約枠の自動生成」 (month picker + result counts). Task record `.ai-collab/tasks/2026-10-22-schedule-generation/`; REVIEW-001 found R-01 (a stored break outside opening hours produced slots outside the hours), R-02 (second-precision boundaries truncated), R-03 (zero-result UI message asserted a cause); fixed in HANDOFF-002 and ACCEPTED in REVIEW-002 (2026-10-22).

**Unit test package (2026-10-23, branch `test/unit-test`)**: full-feature 単体テスト in `test/` (user request: test items + evidence in a project test folder, with Codex). JUnit 342 + JaCoCo (added to build.gradle), 134 API items against a scratch PostgreSQL (all 52 endpoints, docs/07 error codes, 15-route role-denial matrix), 33 screen items (SC-001..SC-016) with captures. Defects found and fixed: D-003..D-005 unknown URL / unsupported method / unsupported Content-Type returned 500 → COMMON-001/002/003; D-008 SERVER-001 message was English; D-007 SC-008 search could not filter by area; D-009 SC-002 hid the API validation/duplicate message; spec fixes D-001 (register errors are VALIDATION-001), D-002 (logout contract), D-006 (petType values). Codex (`gpt-6.1-sol`) reviewed the test design (G-01..G-11 gaps added as items/tests). Task record `.ai-collab/tasks/2026-10-23-unit-test/` (accepted by Codex in REVIEW-003; README reproduction fix R-02 in round 3). Test design by the main session; execution partly by Sonnet subagents (user instruction).

**Frontend review (2026-10-04)**: API wiring is implemented, but review is CHANGES_REQUESTED in `.ai-collab/tasks/2026-10-04-frontend-api-integration/REVIEW-001.md`: F-01 private pet cache survives account changes, F-02 stale hospital schedules can overwrite the current hospital, F-03 same-day past slots remain selectable. Codex verified the frontend build and reproduced F-01/F-02 with the actual stores. Browser/Capacitor evidence remains implementer-reported; native runtime verification is pending in TODO.md. Codex owns only this review and the user-requested TODO/status update; application fixes remain with Claude. Claude fixed F-01..F-03 on 2026-10-04 (session-scoped pet store reset/guard, hospital-keyed reservation data with latest-response-wins hospital store, JST start-time rule `isUpcoming`); response in `HANDOFF-002.md`, ACCEPTED in `REVIEW-002.md` (2026-10-04). Browser and APK evidence there is implementer-reported.

**Done**: Auth (register / login / JWT / refresh rotation / logout / `GET /users/me`) · Pet CRUD · Hospital (create/update/suspend/list/detail) · HospitalSchedule (create/list/block/unblock) · HospitalBusinessHours CRUD · Reservation (create/list/detail/cancel/approve/complete/reject) · HealthRecord CRUD · Notification (list/read + generation on reservation approve/reject/complete, 2026-10-03) · Vaccination (register/list/complete/reschedule + reminder Scheduler, 2026-10-05) · Frontend all screens SC-001〜015 (auth·pet·hospital·reservation·health record·notification·dashboard·logout wired to real API, 2026-10-04) · ProtectedRoute + role guards · token restore on reload.

**Practice gaps**: none open. The user asked on 2026-10-21 to finish all Spring work, so the Vaccination cancel/delete gaps (2026-10-05) were implemented by Claude Code. The same request covers the rest of this round (admin features, pet fields, schedule generation): no new gaps. Later new domains follow §11-1 again unless the user waives it.

**HealthRecord/Notification review (Codex REVIEW-001, 2026-10-03)** in `.ai-collab/tasks/2026-10-03-health-notification/`: partial PATCH erased omitted fields (R-01), and stale full-entity saves could resurrect deleted health records (R-02) or undo a hospital suspension (R-03, pre-existing). Fixed with column-specific conditional updates (§3 rule 5), applied also to the same pattern in Pet and HospitalBusinessHours updates. Response in `HANDOFF-002.md`. Re-review REVIEW-002 (2026-10-04) accepted those and found R-04: a concurrent partial PATCH still wrote back stale values of unsent fields; fixed by reading the health record with a row lock (§3 rule 5). Response in `HANDOFF-003.md`.

**Review round 2 (Codex REVIEW-002, 2026-09-14)**: R-02〜R-08 accepted as resolved; R-01 stayed open because the conditional revoke did not serialize refresh against the per-user bulk revoke in login/logout. Fixed on 2026-09-23 by locking the user row in all three operations (see §3 rule 9); response in `HANDOFF-003.md`. That also closes the previously deferred concurrent-login residual.

**Review round 2026-09-13** (Codex REVIEW-001 → fixes in `.ai-collab/tasks/2026-09-13-implementation-review/HANDOFF-002.md`): R-01〜R-08 addressed — refresh rotation now single-consumption (conditional revoke must affect exactly 1 row), refresh tokens carry `jti`, access/refresh separated by a `type` claim so a refresh token used as Bearer no longer throws inside the filter, reservations are protected by a DB-level partial unique index plus expected-status conditional updates, SUSPENDED hospitals reject new reservations (RESERVATION-009), deleted pets no longer break reservation history, and the frontend refresh queue settles every waiter on failure. **Still unverified**: real PostgreSQL concurrency runs for the race fixes.

**Not started**: django-api entirely (out of scope of the 2026-10-21 "finish Spring" request).

**Branches (2026-10-23)**: all feature branches up to `feat/spring/schedule-generation` were merged into `develop` (PR #20) and deleted locally and on origin; only `main` and `develop` remain, plus the working branch. `develop` is ahead of `main` (release PR pending, by the user). Verify with `git branch --all` and `git log`.

## 11. Workflow rules

1. **Practice gaps**: when adding a new domain, intentionally leave 1–2 features unimplemented; record them in TODO.md under 「あえて未実装のまま残した項目（練習用）」 and mark the docs heading with 「（未実装）」.
2. **Doc sync after implementation changes** (use `/docs-sync`): update TODO.md → affected `docs/` → §10 of this file. Change-managed items: architecture, ERD/tables/columns, API endpoints/request/response, ErrorCodes, Role/Status values, state transitions, JWT/refresh behavior, Docker config, versions.
3. Skills: `/spring-new-domain` (new domain), `/spring-test` (tests), `/docs-sync` (doc sync). Shared skill bodies live in `.agents/skills/`; Claude entry points reference them from `.claude/skills/`. Tool-specific specialist definitions live in `.codex/agents/` and `.claude/agents/`; use them only when delegation is authorized.
4. `docs/` filenames carry Notion export hashes — always locate them by prefix Glob (e.g. `docs/07_API_Design*.md`).

## 12. docs/ index

| No | Doc | Update trigger |
|---|---|---|
| 01 Project_Overview | rarely |
| 02 Requirement_Definition | new features |
| 03 Business_Flow | flow changes |
| 04 System_Architecture | architecture decisions |
| 05 Screen_Design | screen changes |
| 06 ERD | **any DB change** |
| 07 API_Design | **any API/ErrorCode change** |
| 08 State_Design | **any Status/transition change** |
| 09 Security_Design | auth/authz changes |
| 10 Development_Environment | versions/Docker |
| 11 Test_Strategy | test scenarios |
| 12 Trouble_Shooting | newly solved issues |
| 13 Retrospective | as needed |

Known remaining gaps: docs keep Status "Draft" by design; `pets.image_url` is designed but not implemented (image upload policy undecided, docs/06 §8 note).
