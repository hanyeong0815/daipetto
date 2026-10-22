# Handoff 002: 2026-10-21-user-management

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-22T18:50+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: re-review of R-01..R-03
- Consumed review: [REVIEW-001.md](REVIEW-001.md) (not edited)
- Source base SHA: `7ac79e3` (branch `feat/spring/user-management`)
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD`
- Checkout/branch and dirty state: `E:\workspace\daipetto`; new files intent-added; `.vscode/` excluded
- Dirty snapshot: `HANDOFF-002.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`) and `HANDOFF-002-SHA256.txt` (changed files + `scripts/` + the patch). HANDOFF-001 and its manifest are preserved.

## Response to REVIEW-001

| ID | Disposition | Change | Evidence |
|---|---|---|---|
| R-01 [P2] login in flight issues a token after suspension | Fixed | `LoginService`, right after `lockForSessionUpdate`, re-reads the status and fails with AUTH-002 unless it is ACTIVE, before revoking or saving tokens. The re-read is new port method `UserRepository.findStatusById`: a JPQL **column projection** (`select u.status ...`), which always reads the DB. Re-fetching the entity would return the persistence-context copy loaded by `findByEmail`, as Codex noted. Refresh needed no change: suspension revokes all tokens, and refresh's conditional revoke of the presented token then affects 0 rows (AUTH-003), so it cannot issue a successor. The existing race check covers that order. | Unit, H2 and PostgreSQL race, see below |
| R-02 [P2] list stranded on an empty page | Fixed | `AdminUserPage`: a successful response with an empty page while `page > 0 && page >= totalPages` moves to `max(0, totalPages - 1)`, which re-fetches, instead of rendering it. A genuinely empty result at page 0 still shows the empty message. | Browser, see below |
| R-03 [P3] stale status lines | Fixed | AGENTS.md: 「Not started」 no longer lists the user screen. Remaining Spring work there is now hospital affiliation + admin reservation list, and the scheduler. The branch stack includes `feat/spring/user-management` (in review). | — |

## Acceptance evidence (all executed by Claude Code, 2026-10-22)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew test` after the last Java edit | JDK 17, H2 | exit 0 — 281 tests, 0 failures/errors/skips (279 before) |
| R-01 unit | `LoginServiceTest.login_fail_when_suspended_after_first_read` (first read ACTIVE, re-read SUSPENDED → AUTH-002, order lock → re-read, no revoke/save); `login_success` now asserts lock → re-read → revoke → save | Mockito | PASS |
| R-01 cache bypass | `UserPersistenceAdapterTest.find_status_by_id_bypasses_cached_entity`: load the entity, change status with a native UPDATE in the same context; the cached entity still says ACTIVE while `findStatusById` returns SUSPENDED (missing id → null) | H2 `@DataJpaTest` | PASS |
| R-01 real interleaving | `scripts/user-management-acceptance.mjs`, new check ADMIN-T007: psql holds the user row lock, sets SUSPENDED and revokes tokens. HTTP login reads ACTIVE and is observed blocked on `select … from users … for …` in `pg_stat_activity`. Then COMMIT. | bootJar built after the last Java edit, fresh DB `daipetto_um_1021`, port 18081 | 403 AUTH-002, active refresh tokens = 0. Whole script 15/15 |
| Auth regression | `../2026-09-13-implementation-review/scripts/e2e-api.mjs` | same backend | 12/12 |
| R-02 | browser: 21 ACTIVE users seeded via SQL, filter 「利用中」, 「次へ」 to page 2 (1 row: Pad15), 「アカウント停止」 | Vite 5173 → 18081, admin fixture session injected (no password typed) | after reload: 「該当 20名」, 20 rows rendered, no empty message, pager hidden (single page) |
| Frontend build | `frontend/`: `npm run build` | Node 24.17.0 | exit 0 |

Scratch DB dropped; temporary launch configuration reverted.

## Limitations

Unchanged from HANDOFF-001:

- one extra primary-key query per authenticated request
- a stale SPA role/menu after a remote role change (the backend enforces the current role)
- no guard on the last other SYSTEM_ADMIN

Login now does one more small query after the lock.

## Documentation

docs/09 §3-5 (login re-reads status after the lock), docs/11 ADMIN-T007 (login/refresh vs suspension) and ADMIN-T008 (last-row pagination), AGENTS.md (§8 281, R-03 lines), `scripts/README.md`.

## Receiver instructions

Verify `HANDOFF-002-SHA256.txt` from the repository root. Gradle: `GRADLE_USER_HOME=spring-api/.gradle/review-home` with `--offline` if needed. Write the result to `REVIEW-002.md`; do not edit REVIEW-001 or HANDOFF-001.
