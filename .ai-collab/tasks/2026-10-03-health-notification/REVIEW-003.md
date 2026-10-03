# Review 003: HANDOFF-003 acceptance

- Author / reviewer: Codex
- Publication: final
- Date: 2026-10-04, Asia/Seoul
- Consumed handoff: `HANDOFF-003.md`, including its JST-policy revision.
- Source: branch `feat/spring/health-notification`, HEAD `f0faba909345bf8f109141490c82a4ef1e096303` plus the handoff's dirty snapshot.
- Identity: 81/81 entries in `HANDOFF-003-SHA256.txt` matched before and after verification. Patch SHA-256: `43ea238bcad26d44ba79068e72e2ad9a0c036367aecae81c0e4f1d437400caa7`.
- Verdict: **ACCEPTED** for this snapshot and the reviewed change scope.
- No new actionable defect found. No application source or implementer-owned record was modified.

## Finding dispositions

| ID | Disposition | Evidence |
|---|---|---|
| R-04 concurrent partial updates lose omitted-field changes | Resolved | The transactional update begins with an active-row PESSIMISTIC_WRITE read. The merge uses the row read after the previous writer commits. Real PostgreSQL checks retained a concurrent memo during weight-only and empty PATCH, and retained both values when two HTTP PATCHes queued behind the same row lock. |
| T-01 UTC/local-date smoke mismatch | Resolved | Smoke compares Asia/Tokyo dates sampled before and after the request. At approximately 01:05 JST (previous calendar date in UTC), all 14 checks passed. |
| R-01/R-02/R-03 | Remain resolved | All 10 fix-acceptance checks passed, including sequential PATCH preservation, no resurrection after health-record/Pet deletion, hospital suspension/info in both orders, reservation suspension rejection and deleted business-hours handling. |
| D-01 | Remains resolved | Updated ERD/domain inventory retained. New locking/timezone policies are reflected in the relevant design and test documents. |

## Timezone change

Reviewed the JST policy as stated in the handoff and current project docs. Spring sets the JVM default from `ServerTime.ZONE_ID` before application startup. The frontend's date helper uses explicit Asia/Tokyo formatting, and both modified screens call it. Existing JWT epoch timestamps use `Date`/duration arithmetic and do not acquire a nine-hour shift from the default-zone change.

The reviewer deliberately started the jar with `'-Duser.timezone=UTC'`. Startup logs still used `+09:00`, the newly created default recorded_date was `2026-10-04`, and the API smoke passed while UTC was still `2026-10-03`. Fresh Flyway V1-V10 application and Hibernate validation succeeded. This confirms the packaged main-entry-point behavior, not merely a mock of ServerTime.

## Reviewer-executed verification

Environment: Windows, Java 17.0.19, PostgreSQL 16.9. Scripts ran sequentially against isolated `daipetto_review_1003`, port 18081, with `DB` and `BASE` set explicitly. The ordinary development database and server were untouched.

| Command / directory | Actual outcome |
|---|---|
| `./gradlew.bat clean test`, `spring-api/` | Exit 0; XML totals 209 tests, 0 failures/errors/skips |
| `npm run build`, `frontend/` | Exit 0; TypeScript and Vite passed |
| `./gradlew.bat bootJar`, `spring-api/` | Exit 0 |
| Jar with UTC startup property, local profile and scratch DB | Started successfully, migrated V1-V10, validated schema, used JST |
| `smoke-health-notification.mjs` | 14/14 passed |
| `review-notification-checks.mjs` | Access boundaries, first read timestamp preservation, reservation rollback on notification failure and one notification for competing transitions all passed |
| `fix-acceptance.mjs` | 10/10 passed |
| `r04-acceptance.mjs` | 3/3 passed; final two-HTTP-PATCH state `5.30/cough/http-memo/2026-05-20`, both responses 200 |
| September-task `scripts/e2e-api.mjs` | 12/12 auth/reservation regressions passed |
| Final manifest check | 81/81 matched; no source drift |

Except the final row's September script, script filenames above refer to this task's `scripts/` directory, run using Node from the repository root. Every listed script exited 0. The old defect-reproducing script was not re-run because it specifically requires blocking on UPDATE; the corrected implementation blocks earlier on the locking SELECT. The inspected acceptance script waits for either lock shape and asserts the correct final state, including two real HTTP writers.

Execution note: the first unquoted `-Duser.timezone=UTC` invocation was parsed incorrectly by PowerShell and exited before starting Java. Quoting that single JVM argument corrected the command; all runtime evidence above is from the successful invocation.

## Remaining limitations and next action

- PostgreSQL interleavings are manual scripts, not Gradle/CI coverage. The H2 and Mockito tests do not independently establish concurrent behavior; the real database runs above supply that evidence for this snapshot.
- Lock waiting has no explicit timeout, and the health-record lock is taken before owner validation. These are disclosed operational characteristics; rejected requests cannot modify the record. No new authorization bypass or deadlock cycle was found in the reviewed paths.
- JST initialization is in the application main entry point; the ordinary Gradle test JVM does not execute it. Packaged startup was explicitly tested here.
- No browser/mobile E2E or dependency vulnerability scan was run. Frontend changes were inspected and type/build checked. Existing HospitalMapper `suspend` warning, Pet PATCH wording, timestamp-offset representation and pet-fields merge requirements remain previously documented items outside these fixes.
- The reviewer stopped the dedicated backend and dropped the scratch database after validation.
- Claude may consume this acceptance and perform the next user-authorized integration step. This report does not itself commit, merge, push or deploy. Preserve this review and the consumed handoff; any source change needs a new snapshot and appropriately scoped verification.
