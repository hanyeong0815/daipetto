# Daipetto — Claude Code Context

Pet health management + hospital reservation platform. Portfolio for a backend job search in Japan — document/implementation consistency is itself a showcased skill, so keep docs in sync (§11).

This is the **single** auto-loaded context file. The former `.claude/CLAUDE.md` and `claude_code_handoff_daipetto.md` were merged into it (handoff archived at `docs/archive/claude_code_handoff_daipetto.md`, kept for history only — do not read it at session start).

Last updated: 2026-08-23 (branch `fix/auth-session-issues`)

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
5. Prefer explicit `@Modifying` update queries over dirty checking for important state changes.
6. Role checks via `@PreAuthorize("hasAuthority('ROLE_...')")`; SecurityConfig only guarantees `authenticated()`.
7. Owner checks with the `Preconditions.validate` pattern (see `UpdatePetService`).
8. Domain state transitions are methods returning a new immutable instance (see `Hospital.suspend()`, `Reservation.approve()`); Lombok on Domain: `@Getter`/constructors only, never `@Setter`/`@Data`.
9. Auth policy: access token 30 min / refresh token 14 days, refresh **rotation** (old token revoked on every use), max 1 active refresh token per user, stored in PostgreSQL (no Redis in MVP).

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
| USER-002 | 入力バリデーション | 400 |
| USER-003 | 存在しないユーザー | 404 |
| USER-004 | 利用停止中ユーザー | 403 |
| PET-001 | 存在しないペット | 404 |
| PET-002 | 飼い主不一致 | 401 |
| HOSPITAL-001 | 存在しない病院 | 404 |
| HOSPITAL-002 | 存在しない予約枠 / hospitalId不一致 | 404 |
| HOSPITAL-003 | 存在しない営業時間 / hospitalId不一致 | 404 |
| HOSPITAL-004 | 曜日重複登録 | 409 |
| RESERVATION-001 | 重複予約（同一枠にREQUESTED/APPROVED既存） | 409 |
| RESERVATION-002 | 存在しない予約枠 / hospitalId不一致 | 404 |
| RESERVATION-003 | BLOCKED枠への予約 | 409 |
| RESERVATION-004 | 存在しない/他人のペット | 403 |
| RESERVATION-005 | 過去日時の枠 | 400 |
| RESERVATION-006 | 存在しない予約 | 404 |
| RESERVATION-007 | 予約者本人でない | 403 |
| RESERVATION-008 | 不正な状態遷移 | 409 |
| AUTH/USER/PET/HOSPITAL/RESERVATION-999 | domain default | 500 |
| VALIDATION-001 | `@Valid` failure (GlobalExceptionHandler) | 400 |
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

Forbidden: COMPLETED→REQUESTED, CANCELLED→APPROVED, REJECTED→APPROVED, COMPLETED→CANCELLED. Max 1 REQUESTED/APPROVED per schedule_id. Approve/reject/complete should create a Notification (docs/08 §6-6 — Notification domain not built yet).
`hospital_schedules.status` only expresses hospital-side availability; "already booked" is judged from `reservations` (no auto-BLOCK on booking — docs/06 §18).

## 7. DB / Flyway

- DB changes only via Flyway; logical delete (`deleted_at`) is the default.
- Applied on this branch: V1 users, V2 refresh_tokens, V3 pets, V4 hospitals, V5 hospital_business_hours, V6 hospital_schedules, V7 reservations.
- ⚠ Unmerged `feat/spring/pet-fields` also uses **V4** (`V4__alter_pets_add_columns.sql` — breed/neutered/microchip_number). Renumber one side at merge. New migrations start at **V8**; check every branch for number collisions before numbering.
- Not created yet: health_records / vaccinations / notifications / hospital_admins (definitions in docs/06). Because `hospital_admins` is missing, admin APIs cannot verify "own hospital" — hospital-affiliation checks are intentionally skipped for now.

## 8. Test rules

- Method names English, `@DisplayName` Japanese, comments minimal.
- Spring Boot 3.3.13 → use `@MockBean` (never `@MockitoBean`).
- Controller tests: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` + `@Import(GlobalExceptionHandler.class)`. If `JwtAuthenticationFilter` breaks context creation, exclude it via `excludeFilters`. Mock **every** UseCase the controller depends on.
- Principal is `Long`: use `new UsernamePasswordAuthenticationToken(1L, null, authorities)` — `.with(user(...))` fails.
- Unit tests mock repositories (no DB; H2/Testcontainers not introduced). Null-tolerant `@Query` bugs escape mock tests — see docs/12 §3-10.
- Verify: `./gradlew clean test` in `spring-api/` (133 green as of 2026-08-23).

## 9. Environment

| Item | Value |
|---|---|
| Spring | Java 17 / Boot 3.3.13 / Gradle 8.14.4 / Flyway 10.20.x / MapStruct 1.5.5.Final / jjwt 0.12.6 |
| Frontend | React 18.3.1 / TS 5.7.x / Vite 6.4.x / Node 24.17.0 / Tailwind 3.4.x / Zustand 5 / Axios 1.18 / Router 6.30 / Capacitor 8.4 |
| Django | Python 3.12.10 / DRF 3.15.2 (not started) |
| DB | PostgreSQL 16.9 (db/user: daipetto) |
| Ports | frontend 5173 / spring 8080 / django 8000 / postgres 5432 |

Start: `docker compose up -d` (postgres) → `./gradlew bootRun` / `npm run dev`.
API base URL is resolved at runtime per platform via `Capacitor.getPlatform()` (`VITE_API_BASE_URL_WEB` / `_ANDROID` / `_IOS`).

## 10. Status (2026-08-23)

**Done**: Auth (register / login / JWT / refresh rotation / logout / `GET /users/me`) · Pet CRUD · Hospital (create/update/suspend/list/detail) · HospitalSchedule (create/list/block/unblock) · HospitalBusinessHours CRUD · Reservation (create/list/detail/cancel/approve/complete) · Frontend all screens SC-001〜015 (auth·pet·hospital wired to real API) · ProtectedRoute + role guards · token restore on reload.

**Practice gaps** (user implements these personally — do NOT implement unless explicitly asked; list in TODO.md is authoritative):
- Reservation reject: `PATCH /api/v1/admin/reservations/{id}/reject` (docs/07 §9-6 marked 未実装).

**Not started**: HealthRecord / Vaccination / Notification APIs · schedule auto-generation Scheduler (from `hospital_business_hours`) · vaccine reminder Scheduler · django-api entirely · frontend wiring for reservation/health/notification/admin(reservation·user) screens.

**Unmerged branches**: `feat/spring/pet-fields` (pet columns + docs 06/07 updates), `fix/auth-session-issues` (current).

## 11. Workflow rules

1. **Practice gaps**: when adding a new domain, intentionally leave 1–2 features unimplemented; record them in TODO.md under 「あえて未実装のまま残した項目（練習用）」 and mark the docs heading with 「（未実装）」.
2. **Doc sync after implementation changes** (use `/docs-sync`): update TODO.md → affected `docs/` → §10 of this file. Change-managed items: architecture, ERD/tables/columns, API endpoints/request/response, ErrorCodes, Role/Status values, state transitions, JWT/refresh behavior, Docker config, versions.
3. Skills: `/spring-new-domain` (new domain), `/spring-test` (tests), `/docs-sync` (doc sync). Sub-agents in `.claude/agents/` for large cross-area work.
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

Known remaining gaps: docs keep Status "Draft" by design; pets breed/neutered/microchip_number columns exist only on unmerged `feat/spring/pet-fields` (docs/06 §8 note).
