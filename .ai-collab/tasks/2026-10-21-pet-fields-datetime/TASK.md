# Task: Merge pet-fields, pet edit screen, and +09:00 response datetimes

- Task ID: 2026-10-21-pet-fields-datetime
- Coordinator (single writer): Claude Code
- Implementer: Claude Code
- Reviewer: Codex
- Mode: implementation-and-review
- Status: CLOSED
- Updated at: 2026-10-21T23:45+09:00
- User request reference (2026-10-21): "finish all Spring work and the related frontend; Codex reviews; commit+push after acceptance". User decisions in the same session: response datetimes get the JST offset (`+09:00`); pet update stays a full replacement and docs are corrected to say so.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/spring/pet-fields-datetime` (new, from `feat/spring/vaccination`), HEAD `fe8aa0e`, with an uncommitted merge of `feat/spring/pet-fields` (`b83aef4`, MERGE_HEAD)
- Owned files/modules: pet domain/application/persistence/presentation, V13, V14, `common/time/LocalDateTimeJsonSerializer`, affected tests, frontend pet types/API/pages/routes/layout title, docs 02/05/06/07/11/12, TODO.md, AGENTS.md. No concurrent work.

## Requirements

| ID | Input/action | Expected observable result | Verification |
|---|---|---|---|
| AC-01 | Merge `feat/spring/pet-fields` | breed/neutered/microchip_number columns via V13 (renumbered from the colliding V4); create/detail/list/update carry them | compile, unit/WebMvc tests, PG Flyway V1..V14 |
| AC-02 | `PATCH /pets/{id}` | full replacement incl. the new columns through the conditional `updateProfile` (`deleted_at IS NULL`); docs/07 §5-4 describes full replacement | H2 adapter test, PG HTTP |
| AC-03 | Create/update validation | name ≤50, breed ≤100, weight ≥0 and DECIMAL(5,2), microchip ≤15 → 400 VALIDATION-001 instead of 500 | WebMvc test, PG HTTP |
| AC-04 | Create without gender | 201; `pets.gender` nullable (V14) as docs 02/05/06 specify | WebMvc test, PG HTTP, upgrade from V12 |
| AC-05 | Response datetimes | every `LocalDateTime` in JSON as `yyyy-MM-ddTHH:mm:ss+09:00` (JST wall clock unchanged); date-only/time-only fields unchanged | WebMvc test, PG HTTP |
| AC-06 | Frontend | pet edit screen `/pets/:petId/edit` (SC-005 form reused, prefilled, full body sent), 「不明」 gender, breed on the list, detail edit link; notification time display unaffected | build, browser |

## Decisions

- Migrations: pet-fields V4 → V13 (V4..V12 taken); gender nullability fix as a separate V14.
- Pet update remains a full replacement (user decision), so no locked read is needed (nothing is merged from the read row; AGENTS §3 rule 5).
- Offset serialization is a single `@JsonComponent` (no DTO type changes); second precision to match docs/07 §2-7.

## Progress references

- Latest published handoff: HANDOFF-001.md
- Latest final review: REVIEW-001.md (ACCEPTED; R-01 P3 comment drift deferred to the next round)
- Next actor and action: none — delivered
- Blocker: none

## Closure

- Delivered source revision: the merge commit on `feat/spring/pet-fields-datetime` that adds this record (parents `fe8aa0e`, `b83aef4`). Application code and product docs are identical to the HANDOFF-001 snapshot; only post-acceptance status lines in AGENTS.md §10, TODO.md and this file changed.
- Review: REVIEW-001 ACCEPTED
- Verification evidence: HANDOFF-001
- Remaining findings and disposition: R-01 [P3] NotificationsPage formatter comment still describes offset-free input — fixed in the next round (2026-10-21-user-management). Reported-only: PostgreSQL and browser runs (reviewer had no Docker access).
- Integration state: committed and pushed to origin `feat/spring/pet-fields-datetime` by user authorization (2026-10-21); PR by the user
