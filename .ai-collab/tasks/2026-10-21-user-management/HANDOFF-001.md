# Handoff 001: 2026-10-21-user-management

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-21T23:55+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: review only
- Source base SHA: `7ac79e3` (branch `feat/spring/user-management`, created from `feat/spring/pet-fields-datetime`, previous round ACCEPTED)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD`
- Checkout/branch and dirty state: `E:\workspace\daipetto`; new files intent-added; unrelated untracked `.vscode/` excluded
- Dirty snapshot: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`) and `HANDOFF-001-SHA256.txt` (changed files + `scripts/` + the patch)

## Change summary

Backend:

- `GET /api/v1/admin/users` → `SearchUsersService`. Uses `UserRepository.search`/`count`, which are JPQL with null-tolerant filters cast per docs/12 §3-10, `deletedAt is null`, `order by id`, `Pageable`. Paging is clamped: page<0→0, size≤0→20, size>100→100. A blank keyword means no filter. Returns `common/response/PageResponse<AdminUserResponse>` (no password).
- `PATCH /admin/users/{id}/suspend|activate` → `ChangeUserStatusService`. Order: self check (USER-005), then `lockForSessionUpdate(userId)`, then `updateStatus` (conditional `where id and deletedAt is null`; 0 rows → USER-003; same status counts as success), then, when suspending, `refreshTokenRepository.revokeAllByUserId`.
- `PATCH /admin/users/{id}/role` → `ChangeUserRoleService`: self check, then conditional `updateRole`.
- `AdminUserController`: class-level `@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")`. `UserErrorCode.CANNOT_CHANGE_SELF` = USER-005 (400).
- `JwtAuthenticationFilter`: after validating the access token, loads the user. It authenticates only an existing, non-deleted, non-suspended user, with the **current DB role** as the authority. This implements docs/08 §3-4 「API利用不可」; before, a suspended user kept API access for up to 30 minutes. Role changes now apply on the next request.
- `GlobalExceptionHandler`: `MethodArgumentTypeMismatchException` and `HttpMessageNotReadableException` → 400 VALIDATION-001. Unknown enum values in a query or JSON body were 500 everywhere (docs/12 §3-16).

Frontend:

- `AdminUserPage` (SC-015) replaces the mock data with: search on Enter, role and status chips, 20 per page with prev/next, suspend/activate with confirm, and a role `<select>` with confirm. The self row has no actions. The list reloads after every action, success or failure. An effect guard drops stale responses, and a failed load shows retry instead of the empty message.
- Dashboard 「管理メニュー」 for HOSPITAL_ADMIN/SYSTEM_ADMIN. The admin screens had no link anywhere; ユーザー管理 is shown only to SYSTEM_ADMIN.
- `types/auth.ts`: `UserProfile.id` → `userId` (the API returns `userId`, so `user.id` was always undefined), adds `status`, and adds an `AdminUser` type. `api/adminUser.ts`.
- `NotificationsPage` comment fixed (previous round's REVIEW-001 R-01 P3).

## Acceptance evidence (all executed by Claude Code, 2026-10-21)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew test` after the last Java edit | JDK 17, H2 | exit 0 — 279 tests, 0 failures/errors/skips (261 before; +2 adapter, +8 service, +6 controller, +2 filter) |
| Frontend build | `frontend/`: `npm run build` after the last edit | Node 24.17.0 | exit 0 |
| AC-01..AC-05 on PostgreSQL | `scripts/user-management-acceptance.mjs`, bootJar built after the last Java edit | docker `postgres:16.9`, fresh DB `daipetto_um_1021`, port 18081 | 14/14 |
| Race: suspension vs in-flight refresh | same script, last check | same | suspension observed blocked in `pg_stat_activity` on the user row lock held by psql. After commit, 200 and the successor token is `revoked=true` |
| Auth regression after the filter change | `../2026-09-13-implementation-review/scripts/e2e-api.mjs` on the same backend | same | 12/12 (concurrent refresh single-consumption, refresh-as-bearer 401, booking/transition races) |
| AC-06 browser | Vite 5173 → 18081, admin fixture session injected as a refresh token (no password typed) | built-in browser | Dashboard shows 管理メニュー (3 items for SYSTEM_ADMIN). ユーザー管理 lists 6 users, the self row shows 「（自分）」 and no actions. Search 「sato」 + Enter → 1 row. 「アカウント停止」 → 停止中 + 「停止解除」. Role select → 病院管理者. 「停止解除」 → DB `ROLE_HOSPITAL_ADMIN/ACTIVE`. |

Scratch DB dropped; temporary launch configuration reverted.

## Limitations

- One extra primary-key query per authenticated request (filter). Acceptable at this scale; a cache would reintroduce staleness.
- The SPA's stored profile (`authStore.user.role`) is not refreshed after a role change. Admin screens may stay visible to a demoted user, but every admin API call returns 403. A promoted user sees the new menu after re-login.
- No protection against demoting or suspending the last other SYSTEM_ADMIN; only self-changes are blocked, so the acting admin always remains.
- Deleted users have no API (no user deletion exists); they are excluded from search and return USER-003 on changes.
- `WebMvcTest` slices exclude the JWT filter, so role enforcement (`@PreAuthorize`) is evidenced by the PostgreSQL HTTP run, not by unit tests.

## Documentation

- docs/07: §11-4 implemented, plus §11-5 activate, §11-6 role, §11-7 list (query/paging/response); role table row.
- docs/05: SC-015 functions, self rule, and the dashboard entry point.
- docs/08: §3-3 API links and the self rule; §3-4 how 「API利用不可」 is enforced.
- docs/09: §3-5 suspension revokes refresh tokens; the per-request user check in the filter and its cost.
- docs/02: FR-ADMIN-001.
- docs/11: ROLE-T007, ADMIN-T001..T006.
- docs/12: §3-16 unknown enum → 500.
- TODO.md and AGENTS.md: §3 rule 9, §5 USER-005, §8 279, §10.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| 2026-10-21-pet-fields-datetime R-01 [P3] | Fixed | `frontend/src/pages/NotificationsPage.tsx` comment now describes `+09:00` input | none |

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. If the default Gradle home is inaccessible, use `GRADLE_USER_HOME=spring-api/.gradle/review-home` with `--offline`. PostgreSQL steps are in `scripts/README.md` (new scratch DB only, port other than 8080). Write findings to `REVIEW-001.md` in this directory.
