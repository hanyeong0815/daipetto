# Review 001: 2026-10-21-pet-fields-datetime

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-21T23:32+09:00
- Consumed handoff: HANDOFF-001.md, TASK.md and scripts/
- Reviewed source: `git diff HEAD` on `feat/spring/pet-fields-datetime`; HEAD `fe8aa0eb565baa9321b0cc8987f476e05c192d86`, MERGE_HEAD `b83aef43f9cba42fbb7a7d6ba61a550b266d789d`.
- Dirty snapshot: all 38 entries in HANDOFF-001-SHA256.txt matched from the repository root before review and after verification, including HANDOFF-001.patch. Refs remained unchanged.
- Verdict: **ACCEPTED**
- Scope: AC-01..AC-06; migration collisions, conditional writes, validation, serialization, constructor callers/tests, frontend create/edit flow, docs 02/05/06/07/11/12, TODO.md and AGENTS.md. Full replacement and JST-offset responses were treated as settled user decisions.

## Findings

No blocking correctness findings.

### R-01 [P3] Notification formatter comment describes the old response contract

- File and line: `frontend/src/pages/NotificationsPage.tsx:13`.
- Trigger / reproduction: compare the formatter comment with the new serializer and docs/07 section 2-7. The comment still describes an offset-free JST datetime.
- Expected and observed behavior: the comment should describe offset-bearing input. Line 14 still correctly displays `2026-10-21T23:59:58+09:00` as `2026-10-21 23:59` by slicing the wall-clock prefix.
- User or system impact: misleading maintenance guidance only; no display regression.
- Evidence and confidence: direct source inspection; high confidence.
- Suggested correction and regression check: update the comment; retain wall-clock display behavior and check the example above.
- Disposition: deferred, nonblocking housekeeping. Comment-only drift has no material runtime risk and does not prevent acceptance.

## Verification

Reviewer commands used the frozen snapshot on Windows, from `E:/workspace/daipetto` unless stated otherwise. OpenJDK 17.0.19; Node 24.17.0.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | `Get-FileHash -Algorithm SHA256` for every manifest entry; `git rev-parse HEAD MERGE_HEAD` | 38/38 matched twice; refs matched | None needed | Unrelated untracked `.vscode/` and `spring-api/.claude/` excluded |
| AC-01 migrations | `git for-each-ref` and `git ls-tree` across all 28 local/remote-tracking refs; compare old V4 and V13 blob hashes | No existing V13/V14 in available refs. Only pet-fields has the colliding pet V4. Old V4 and new V13 both hash to `1fa573a817d184159c133a6deb88865f5b927184`. Working migrations run V1..V14; V14 only drops gender NOT NULL | Fresh PostgreSQL V1..V14 and V12 upgrade preserving an existing pet | Remote refs inspected locally, not fetched; PostgreSQL not rerun |
| Spring suite | `spring-api/`: `./gradlew.bat clean test --offline`, with `GRADLE_USER_HOME=E:/workspace/daipetto/spring-api/.gradle/review-home` | `BUILD SUCCESSFUL in 56s`; fresh UTF-8 XML totals: **261 tests, 0 failures, 0 errors, 0 skipped**. Execution tool retained its session after success, so no process exit code was returned | Handoff also reports 261 passing | Default-home attempt exited 1: inaccessible `C:/.gradle` lock directory. Existing Hospital/Vaccination mapper warnings remain |
| AC-02 conditional update | Trace controller/service/domain/adapter/JPQL and H2 tests | All eight profile fields plus updatedAt written; deletedAt/userId/createdAt not rewritten. Deleted-row predicate and affected-row failure retained. Mapping and deleted-row regression tests pass | PostgreSQL omitted-field clearing and full replacement checks | No reviewer PostgreSQL concurrency run |
| AC-03/04 validation | Compare DTO annotations, controller `@Valid`, V3/V13/V14 and controller tests | Matching name 50, breed 100, microchip 15, nonnegative DECIMAL(5,2) limits; gender optional. Constructor callers compile and tests pass | Invalid PATCH and actual nullable-gender insertion | H2 schema generation does not prove Flyway constraints |
| AC-05 datetime | Inventory response DTOs/Jackson config; NotificationControllerTest in suite | Response LocalDateTime fields are pet detail, hospital detail and notification createdAt. Global serializer covers all three. Test asserts fractional-second truncation and exact +09:00 output. No competing field formatter. LocalDate/LocalTime and deserializers are not changed | PostgreSQL pet/notification timestamps and plain birthDate | Hospital offset and isolated date/time/deserialization behavior lack dedicated new assertions; unchanged registrations verified by inspection |
| AC-06 frontend | `frontend/`: `npm.cmd run build`; inspect routes/form/API/store/display callers | Exit 0; TypeScript/Vite passed. Form keyed by pet ID; inactive responses ignored; failed load blocks submission. Every field prefilled; species mapped to petType; empty optional values clear as documented; null gender becomes unknown | Browser create/edit/preservation and notification display | Browser/native runtime not rerun |
| Documents | Specified document diffs versus code; `git -c core.safecrlf=false diff HEAD --check` | No blocking mismatch; diff check exit 0. HealthRecord weight wording also matches its existing DTO validation | None needed | AGENTS/TODO describe working-tree integration as merged; Git merge remains uncommitted |
| PostgreSQL availability | `docker version` | Client present; engine access denied at `npipe:////./pipe/docker_engine` | Handoff 17/17 acceptance checks and V12 upgrade | All PostgreSQL evidence remains reported-only |

## Remaining risks and next action

- Acceptance covers this exact manifest snapshot. H2/mocks do not prove PostgreSQL migrations or concurrency. Browser evidence is implementer-reported; Android/iOS runtime remains unverified.
- Whole-profile last-writer-wins follows the user's full-replacement decision. Conditional writes preserve deletion. No partial-update redesign is requested.
- Only this review record was created by the reviewer; source/docs/task/handoff/scripts and merge state were not changed. Authorized test/build output was generated. `spring-api/.gradle/review-home` was retained.
- Next actor: Claude Code/coordinator should consume this review, record acceptance at the next handoff boundary, and handle integration under the user's authorization. R-01 can be corrected as routine comment maintenance. This review does not commit, finish, abort or publish the merge.
