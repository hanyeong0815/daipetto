# Task: SC-015 user management (search, suspend/activate, role change)

- Task ID: 2026-10-21-user-management
- Coordinator (single writer): Claude Code
- Implementer: Claude Code
- Reviewer: Codex
- Mode: implementation-and-review
- Status: CLOSED
- Updated at: 2026-10-22T19:10+09:00
- User request reference (2026-10-21): "finish all Spring work and the related frontend; Codex reviews; commit+push after acceptance"; no practice gaps this round.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/spring/user-management` (new, from `feat/spring/pet-fields-datetime` at `7ac79e3`)
- Owned files/modules: user application/persistence/presentation (admin parts), `PageResponse`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`, `UserErrorCode` (USER-005), frontend `AdminUserPage`, `api/adminUser.ts`, `types/auth.ts`, dashboard admin menu, `NotificationsPage` comment, docs 02/05/07/08/09/11/12, TODO.md, AGENTS.md.

## Requirements

Specs: docs/05 SC-015 (search, suspend, role change), docs/07 §11-4 (suspend, previously 「未実装」), docs/08 §3 (ACTIVE⇔SUSPENDED by SYSTEM_ADMIN; SUSPENDED = no login, **no API use**), docs/09 §13-1 (user stop/restore).

| ID | Input/action | Expected observable result | Verification |
|---|---|---|---|
| AC-01 | `GET /admin/users` (SYSTEM_ADMIN) | keyword (email/nickname, case-insensitive), role, status filters; non-deleted; id order; `PageResponse`; no password; PostgreSQL null params work | H2, service, WebMvc, PG HTTP |
| AC-02 | `PATCH /admin/users/{id}/suspend` | status SUSPENDED (idempotent), all refresh tokens revoked under the session lock; the same access token is rejected on the next request | service, filter unit, PG HTTP + race |
| AC-03 | `PATCH /admin/users/{id}/activate` | ACTIVE (idempotent); user can log in again | service, PG HTTP |
| AC-04 | `PATCH /admin/users/{id}/role` | role changed; effective on the target's next request | service, filter unit, PG HTTP |
| AC-05 | self / missing / bad input | self → 400 USER-005; missing or deleted → 404 USER-003; unknown enum in query or body → 400 VALIDATION-001; non-SYSTEM_ADMIN → 403 | WebMvc, PG HTTP |
| AC-06 | Frontend | SC-015 page on the real API; admin menu on the dashboard | build, browser |

## Decisions

- docs/08 §3-4 requires suspended users to lose API access. JWT access tokens are stateless, so `JwtAuthenticationFilter` now loads the user per authenticated request. It rejects suspended, deleted or missing users and takes authorities from the current DB role. Cost: one primary-key lookup per request.
- Suspension takes `lockForSessionUpdate` (same as login/refresh/logout) before updating, so an in-flight refresh cannot leave a successor token.
- Same-state suspend/activate succeeds (idempotent) rather than returning a conflict.
- Out-of-range paging is clamped instead of rejected.

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001)
- Latest final review: REVIEW-002.md (ACCEPTED; R-01..R-03 resolved, no new findings)
- Next actor and action: none — delivered
- Blocker: none

## Closure

- Delivered source revision: the commit on `feat/spring/user-management` that adds this record. Application code and product docs equal the HANDOFF-002 snapshot; only post-acceptance status lines in AGENTS.md §10, TODO.md and this file changed.
- Review: REVIEW-001 CHANGES_REQUESTED → HANDOFF-002 → REVIEW-002 ACCEPTED
- Verification evidence: HANDOFF-001, HANDOFF-002, REVIEW-001, REVIEW-002
- Remaining findings and disposition: none open. Reported-only: PostgreSQL and browser runs (reviewer had no Docker access).
- Integration state: committed and pushed to origin `feat/spring/user-management` by user authorization; PR by the user
