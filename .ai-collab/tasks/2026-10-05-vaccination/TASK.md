# Task: Vaccination API and reminder Scheduler

- Task ID: 2026-10-05-vaccination
- Coordinator (single writer): Claude Code
- Implementer: Claude Code
- Reviewer: Codex
- Mode: implementation-and-review
- Status: CLOSED
- Updated at: 2026-10-21T23:40+09:00
- User request reference: "check TODO for remaining Spring work; otherwise start django-api" → user chose "Vaccination API first", practice gaps = cancel + delete, reminder Scheduler included in this round.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/spring/vaccination` (new, stacked on `feat/react-api-integration`), initial HEAD `6e18153cd7010c73a5771285b045cb3bdc3e55aa`
- Owned files/modules: `spring-api/**/vaccination/**`, `infrastructure/scheduler/`, `V11__create_vaccinations.sql`, `Notification.forVaccination`, frontend `VaccinationPage`, `api/vaccination.ts`, `types/vaccination.ts`, `PetDetailPage` vaccination panel, `DetailLayout` title, docs 02/05/06/07/08/11, TODO.md, AGENTS.md. No concurrent work.

## Requirements

Specs: docs/06 §14 (vaccinations table), docs/08 §7 (SCHEDULED/COMPLETED/CANCELLED, CANCELLED→SCHEDULED re-registration), docs/08 §11 (daily 09:00, SCHEDULED within 7 days, no duplicate notification), docs/11 SCHD-T001..T005. docs/07 had no Vaccination API; §6-5..§6-11 were added in this task.

| ID | Input/action | Expected observable result | Planned verification |
|---|---|---|---|
| AC-01 | `POST /api/v1/pets/{petId}/vaccinations` by pet owner | 201, row with status SCHEDULED; non-owner/missing pet → VACCINATION-002 (403); blank name / missing date → VALIDATION-001 | Service + Controller tests |
| AC-02 | `GET /api/v1/pets/{petId}/vaccinations` | Non-deleted rows ordered by scheduled_date asc; non-owner → VACCINATION-002 | Service + Controller tests |
| AC-03 | `PATCH /vaccinations/{id}/complete` (body optional) | SCHEDULED→COMPLETED with completedDate (default today JST, future → 400); other states or lost race → VACCINATION-003 (409); missing → 001 | Service, Controller, H2 adapter, PG |
| AC-04 | `PATCH /vaccinations/{id}/reschedule` | CANCELLED→SCHEDULED with new scheduledDate, completedDate cleared; other states/lost race → 003 | Service, H2 adapter, PG |
| AC-05 | Reminder Scheduler (09:00 Asia/Tokyo) | VACCINATION notification to the pet owner for each SCHEDULED, non-deleted vaccination of a non-deleted pet with scheduled_date in [today, today+7], unless a notification for it exists with created_at >= vaccination.updated_at | Service, H2 adapter, PG |
| AC-06 | V11 migration | vaccinations table + `fk_notifications_vaccination`; Flyway V1..V11 + `ddl-auto: validate` pass on PostgreSQL 16.9 | PG throwaway DB |

## Exclusions and constraints

- Practice gaps (user implements; do not implement or report as missing): `PATCH /vaccinations/{id}/cancel` and `DELETE /vaccinations/{id}`. Docs mark them 「（未実装）」.
- No hospital-admin involvement; vaccinations are owner-managed records.

## Decisions

- Ownership via pet (same as HealthRecord); VACCINATION-001/002/003/999 added.
- State transitions write only owned columns with expected status in WHERE (AGENTS §3 rule 5); no full-entity save.
- Reminder dedupe uses `notification.created_at >= vaccination.updated_at` so a rescheduled vaccination is reminded again; no schema change needed.
- Reminder message includes pet name, vaccine name and date (`Notification.forVaccination`); title stays the NotificationType default.
- Reminder service writes through `NotificationRepository` directly (no new notification UseCase).

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001)
- Latest final review: REVIEW-002.md (ACCEPTED; R-01..R-04 resolved, no new findings)
- Next actor and action: none — delivered
- Blocker: none

## Closure

- Delivered source revision: the commit on `feat/spring/vaccination` that adds this record. Application code and product docs are identical to the HANDOFF-002 snapshot; only the post-acceptance status lines in AGENTS.md §10, TODO.md and this file changed afterwards.
- Review: REVIEW-001 CHANGES_REQUESTED → HANDOFF-002 → REVIEW-002 ACCEPTED
- Verification evidence: HANDOFF-001, HANDOFF-002, REVIEW-001, REVIEW-002
- Remaining findings and disposition: none open. Reported-only: PostgreSQL runs and browser checks (reviewer had no Docker access)
- Integration state: committed and pushed to origin `feat/spring/vaccination` by user authorization (2026-10-21); PR by the user

## Scope change (2026-10-21)

User request: "finish all Spring work and the related frontend; Codex reviews; Claude implements the former practice gaps". Added:

| ID | Input/action | Expected observable result | Verification |
|---|---|---|---|
| AC-07 | `PATCH /vaccinations/{id}/cancel` | SCHEDULED→CANCELLED, `updated_at` advanced; other states or lost race → VACCINATION-003; missing → 001; non-owner → 002 | Service, Controller, H2 adapter, PG HTTP + race |
| AC-08 | `DELETE /vaccinations/{id}` | soft delete from any status, hidden from list and reminders; deleted concurrently → 001; non-owner → 002 | Service, Controller, H2 adapter, PG HTTP + race |
| AC-09 | Frontend SC-016 + SC-006 panel | list/register/complete/cancel/reschedule/delete wired; stale pet response ignored; errors shown and list reloaded | build + browser |

The exclusion "practice gaps … do not implement" above is superseded by this section.
