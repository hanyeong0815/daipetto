# Task: reservation-slot auto-generation (monthly scheduler + manual API), SC-014 button

- Task ID: 2026-10-22-schedule-generation
- Coordinator (single writer): Claude Code
- Implementer: Claude Code
- Reviewer: Codex
- Mode: implementation-and-review
- Status: CLOSED
- Updated at: 2026-10-22T21:11+09:00
- User request reference (2026-10-21): "finish all Spring work and the related frontend; Codex reviews; commit+push after acceptance"; no practice gaps this round. Last Spring unit of that request.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/spring/schedule-generation` (new, from `feat/spring/hospital-admins` at `019ea93`, previous round ACCEPTED)
- Owned files/modules: hospital domain (`HospitalBusinessHours`, `HospitalErrorCode`, ports), hospital schedule persistence, `HospitalScheduleGenerator`, `HospitalScheduleGenerationService`, `HospitalScheduleGenerationScheduler`, `HospitalScheduleAdminController`, frontend `AdminHospitalPage`, `api/hospital.ts`, `types/hospital.ts`, `utils/date.ts`, docs 02/04/05/06/07/08/09/11/12, TODO.md, AGENTS.md.

## Requirements

Specs: docs/04 §14 (monthly scheduler, next month), docs/06 §9-1/§18 (generation rules: split by slot length, break slots BLOCKED, closed weekdays skipped, never overwrite manual BLOCKED), TODO.md 「予約枠自動生成バッチ（設計決定）」 (2026-08-02 design decision with the user), docs/07 §19 (listed as future API).

| ID | Input/action | Expected observable result | Verification |
|---|---|---|---|
| AC-01 | Business hours with a break | open→break and break-end→close split into AVAILABLE slots, the break split into BLOCKED slots, remainders dropped, no slot crosses midnight | domain unit, PG |
| AC-02 | Days without business hours / with any existing slot | not generated; existing (incl. manual BLOCKED) untouched; reported as skipped | unit, PG |
| AC-03 | Re-run / concurrent runs for the same hospital-month | no additional or duplicate slots (hospital row lock) | unit (order), PG concurrency + lock race |
| AC-04 | `POST /admin/hospitals/{id}/schedules/generate {month}` | this month..+12 (this month from today, JST), else HOSPITAL-010; affiliation-checked; counts in the response | unit, WebMvc, PG HTTP |
| AC-05 | Monthly cron | 1st 00:00 JST, next month, ACTIVE hospitals only, per-hospital transaction, one failure does not stop others | unit |
| AC-06 | Business hours create/update | invalid values (open>=close, slot outside 5..240, half/outside break) → 400 HOSPITAL-009 | domain unit, PG HTTP |
| AC-07 | Frontend | SC-014 month picker + 「生成する」, result message, schedule list refreshed | build, browser |

## Decisions

- Skip granularity is the whole day: a day with any slot is not generated (simplest rule that satisfies "never overwrite manual BLOCKED"; partial fill of a day is not attempted).
- Segments are split independently from their own start (docs/06 §18), not on a single grid from the opening time.
- The manual API also works for SUSPENDED hospitals; the cron targets ACTIVE ones only.
- Business-hours validation added because generation now consumes the values (0-minute slots or reversed times were accepted before).

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001)
- Latest final review: REVIEW-002.md (ACCEPTED; R-01..R-03 resolved, no new findings)
- Next actor and action: none — delivered
- Blocker: none

## Closure

- Delivered source revision: the commit on `feat/spring/schedule-generation` that adds this record. Application code and product docs equal the HANDOFF-002 snapshot; only post-acceptance status lines in AGENTS.md §10, TODO.md and this file changed.
- Review: REVIEW-001 CHANGES_REQUESTED → HANDOFF-002 → REVIEW-002 ACCEPTED
- Verification evidence: HANDOFF-001, HANDOFF-002, REVIEW-001, REVIEW-002
- Remaining findings and disposition: none open. Reported-only: PostgreSQL and browser runs (reviewer had no Docker access); the cron trigger itself is unit-tested only.
- Integration state: committed and pushed to origin `feat/spring/schedule-generation` by user authorization; PR by the user
