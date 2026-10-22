# Task: hospital affiliation (hospital_admins), admin reservation list, SC-013/SC-014

- Task ID: 2026-10-22-hospital-admins
- Coordinator (single writer): Claude Code
- Implementer: Claude Code
- Reviewer: Codex
- Mode: implementation-and-review
- Status: CLOSED
- Updated at: 2026-10-22T19:55+09:00
- User request reference (2026-10-21): "finish all Spring work and the related frontend; Codex reviews; commit+push after acceptance"; no practice gaps this round. Resumed 2026-10-22 after the Codex usage limit reset.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/spring/hospital-admins` (new, from `feat/spring/user-management` at `71c0fe8`)
- Owned files/modules: hospital and reservation application/persistence/presentation (admin parts), `ChangeUserRoleService`, `GlobalExceptionHandler`, `HospitalErrorCode`, migration V15, frontend `AdminReservationPage`, `AdminHospitalPage`, `api/hospital.ts`, `api/reservation.ts`, `types/hospital.ts`, `types/reservation.ts`, docs 02/05/06/07/09/11/12, TODO.md, AGENTS.md.

## Requirements

Specs: docs/06 §11 (hospital_admins, previously 「未作成」), docs/05 SC-013 (reservation management list + approve/reject/complete) and SC-014, docs/07 §9-5/9-6 (affiliation check was explicitly skipped as a stopgap), AGENTS.md §7 (stopgap note).

| ID | Input/action | Expected observable result | Verification |
|---|---|---|---|
| AC-01 | V15 | `hospital_admins` (user FK, hospital FK, unique pair, index on hospital_id) applies on PostgreSQL | Flyway on a fresh DB |
| AC-02 | Hospital-managing APIs as HOSPITAL_ADMIN | allowed only for linked hospitals, otherwise 403 HOSPITAL-005 with no change; SYSTEM_ADMIN allowed everywhere; reservation transitions check the reservation's hospital | service, WebMvc, PG HTTP |
| AC-03 | `GET /admin/hospitals/mine` | SYSTEM_ADMIN: every non-deleted hospital incl. SUSPENDED; HOSPITAL_ADMIN: linked only | service, PG HTTP |
| AC-04 | `GET/POST/DELETE /admin/hospitals/{id}/admins` (SYSTEM_ADMIN) | list; link (201; HOSPITAL-001/USER-003/HOSPITAL-006/HOSPITAL-007; concurrent duplicates → one row); unlink (HOSPITAL-008) | service, H2, PG HTTP |
| AC-05 | role change away from HOSPITAL_ADMIN | all links deleted; a link request racing the demotion leaves no link | service, PG race |
| AC-06 | `GET /admin/reservations?hospitalId=&status=&date=` | linked hospital's non-deleted reservations with nickname/pet, ordered by date/start/id; null filters work on PostgreSQL; missing/bad params 400 | H2, service, WebMvc, PG HTTP |
| AC-07 | Frontend | SC-013 on the real API (was mock data); SC-014 lists managed hospitals and has the SYSTEM_ADMIN hospital-admin section | build, browser |

## Decisions

- Affiliation is checked in application services through `HospitalAccessChecker` (current DB role + `hospital_admins`), before any other validation. A HOSPITAL_ADMIN probing an unlinked or nonexistent hospital gets HOSPITAL-005, not 404.
- Link management is SYSTEM_ADMIN only. Only ROLE_HOSPITAL_ADMIN users can be linked. Demotion deletes links and promotion does not restore them.
- The admin list is unpaged (one hospital); status filtering exists in the API, while the screen filters client-side to show per-status counts.
- SC-014 hospital admin candidates: first 100 ACTIVE HOSPITAL_ADMIN users (`ponytail:` comment in code).

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001)
- Latest final review: REVIEW-002.md (ACCEPTED; R-01..R-03 resolved, no new findings)
- Next actor and action: none — delivered
- Blocker: none

## Closure

- Delivered source revision: the commit on `feat/spring/hospital-admins` that adds this record. Application code and product docs equal the HANDOFF-002 snapshot; only post-acceptance status lines in AGENTS.md §10, TODO.md and this file changed.
- Review: REVIEW-001 CHANGES_REQUESTED → HANDOFF-002 → REVIEW-002 ACCEPTED
- Verification evidence: HANDOFF-001, HANDOFF-002, REVIEW-001, REVIEW-002
- Remaining findings and disposition: none open. Reported-only: PostgreSQL and browser runs (reviewer had no Docker access).
- Integration state: committed and pushed to origin `feat/spring/hospital-admins` by user authorization; PR by the user
