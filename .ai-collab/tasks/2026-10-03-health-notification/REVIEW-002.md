# Review 002: response to HANDOFF-002

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-04, Asia/Seoul
- Consumed handoff: `HANDOFF-002.md`
- Source: `feat/spring/health-notification`, HEAD `f0faba909345bf8f109141490c82a4ef1e096303`, plus `HANDOFF-002.patch` dirty snapshot.
- Identity: all 71 entries in `HANDOFF-002-SHA256.txt` matched before review and at completion.
- Verdict: **CHANGES_REQUESTED** for R-04. Previous R-01/R-02/R-03 and D-01 are resolved within their original reproduction scope.
- No application source or implementer-owned handoff was edited.

## Previous findings

| ID | Disposition | Reviewer evidence |
|---|---|---|
| R-01 omitted fields erased in sequential PATCH | Resolved | Memo-only, weight-only, empty and blank-text PATCH acceptance checks passed. Null/omission preserves existing values, blank text clears, weight cannot be cleared; policy is documented. Concurrent preservation remains a separate issue below. |
| R-02 HealthRecord resurrected after deletion | Resolved | Conditional active-row UPDATE affects zero rows after deletion commits; real HTTP PATCH returned 404 HEALTH-001 and deleted record remained absent. |
| R-03 hospital info update undoes suspension | Resolved | Info and status update disjoint columns. Both controlled interleavings passed; hospital remained SUSPENDED, new information survived, new reservations returned RESERVATION-009. |
| D-01 stale domain inventory | Resolved | ERD inventory now says V9/V10, AGENTS no longer calls Notification unbuilt, relevant frontend TODO backend-status claims corrected. |

Pet and HospitalBusinessHours received the equivalent conditional-update fix. Their delete/update interleavings returned PET-001/HOSPITAL-003 without restoring rows. Existing owner checks and service transactions remain in place.

## R-04 [P2] Concurrent partial updates overwrite fields absent from the request

- Location: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/persistence/healthrecord/HealthRecordJpaRepository.java:24-26`; caller `application/healthrecord/service/UpdateHealthRecordService.java:33-42` under the same Java root.
- The service merges omitted values from a previously read domain object, then `updateContent` writes every content column. Its deletion predicate prevents resurrection but does not preserve concurrent edits to omitted fields.
- Trigger: the same owner edits weight and memo through overlapping requests (for example, two tabs/devices or independently autosaved fields). A memo update commits while a weight-only PATCH is between read and write. The PATCH writes its old memo back, despite never receiving memo in its body. This is a lost update to a different field, not two competing values supplied for the same field.
- Deterministic reviewer reproduction: create `weight=4.70, memo=original`; hold `UPDATE health_records SET memo='concurrent-memo'` uncommitted in SQL; issue real HTTP `PATCH {"weight":5.3}`; observe its UPDATE waiting in pg_stat_activity; commit the memo transaction. PATCH returns 200, but final state is `5.30/original` instead of `5.30/concurrent-memo`.
- Script: `scripts/review-002-lost-update.mjs`, executed successfully and then re-executed alone. Assertions deliberately confirm the defect. As in the earlier reproduction, one competing mutation is SQL and the PATCH goes through the real Spring HTTP stack.
- HANDOFF-002 explicitly acknowledges record-level last-writer-wins. This is transparent disclosure, but a single owner does not prevent overlapping writes, and no user acceptance of this data-loss limitation is recorded. docs/07 section 6-3 promises that omitted fields preserve existing values.
- Suggested fix: send supplied-field intent to the persistence update and use per-field CASE/flags to keep current database values on omission while allowing blank-text clearing; alternatively serialize read/merge/write with an appropriate row lock or reject stale updates using version checks. Preserve the active-row predicate. Add a two-field concurrent PATCH regression; an empty PATCH must not undo a concurrent content update either.
- Disposition: open; reviewer reproduced, high confidence. This is a residual behavior identified during re-review, not a regression introduced by the conditional UPDATE itself.

## T-01 [P3] Smoke date assertion compares UTC with server-local date

- Location: `scripts/smoke-health-notification.mjs:62` (the `new Date().toISOString().slice(0, 10)` comparison).
- Reviewer ran the unchanged smoke after midnight Korea time: stored recorded_date was `2026-10-04`; the JavaScript UTC date was `2026-10-03`. Result: **13/14, exit 1**, although `HealthRecordService` correctly defaults to the server-local `LocalDate.now()`.
- This is a test-oracle timezone mismatch, not proof of an API regression. It recurs during the first nine hours of each Korea/Japan day.
- Use the API's defined business/server timezone for the expected date and account for a date boundary during the request. If the intended API policy is UTC instead, explicitly align service, specification and test. Do not change the production date merely to satisfy this assertion.
- Disposition: open; nonblocking test correction, separate from R-04.

## Reviewer-executed verification

Environment: Windows, Java 17.0.19, PostgreSQL 16.9; isolated `daipetto_review_1003`, API port 18081. Development database and usual server were not used.

| Command / location | Outcome |
|---|---|
| Hash verification, repository root | 71/71 match |
| `./gradlew.bat clean test`, `spring-api/` | Exit 0; XML totals 208 tests, 0 failures/errors/skips |
| `./gradlew.bat bootJar`, `spring-api/` | Exit 0 |
| Boot jar with local profile, scratch DB, port 18081 | Fresh V1-V10 migration and entity validation succeeded; HTTP requests served |
| `node .../scripts/smoke-health-notification.mjs`, repository root | Exit 1; 13/14, only UTC/local-date comparison failed (T-01) |
| `node .../scripts/review-notification-checks.mjs` | Exit 0; access boundaries, read timestamp idempotency, notification-failure rollback and single notification for competing transitions passed |
| `node .../scripts/fix-acceptance.mjs` | Exit 0; 10/10 including prior defects, reverse hospital race and equivalent Pet/Hours races |
| `node .../scripts/review-002-lost-update.mjs` | Exit 0; R-04 reproduced, final `200 5.30/original`; this is a defect assertion, not acceptance |

Here `...` means `.ai-collab/tasks/2026-10-03-health-notification`. Database scripts ran with `DB=daipetto_review_1003` and `BASE=http://localhost:18081`. Run smoke first for fixtures, notification checks next, and fix-acceptance after that: the latter adds a pet and invalidates the notification harness's single-pet assumption. The dedicated backend was stopped and the scratch database dropped after review.

The frontend was unchanged; its previously successful build was not re-run. The separate 12-check auth/reservation regression is implementer-reported in this handoff, not personally re-run this round. The current full Spring suite and changed-path integration checks were personally run. No browser/mobile test or vulnerability scan was performed.

## Remaining notes and next action

- Existing HospitalMapper `suspend` warning persists; no new mapping failure was found.
- Existing Pet PATCH semantics and the unmerged pet-fields merge requirements are disclosed in HANDOFF-002/TODO; this re-review does not silently approve or implement their product-policy decisions.
- Claude Code should address R-04 and T-01, retain the resolved deletion/suspension regressions, and publish the next immutable handoff. Do not overwrite HANDOFF-002 or this review. No merge or deployment is implied.
