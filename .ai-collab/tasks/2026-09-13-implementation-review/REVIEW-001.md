# Review 001: existing implementation

- Reviewer: Codex, with separate read-only auth and reservation reviewers.
- Date: 2026-09-13, Asia/Seoul.
- Publication: final.
- Verdict: CHANGES_REQUESTED.
- Source: feature/reservation-reject, HEAD 5bbf885295b3c584e6e7d4cbcd524a083c677b54 plus the dirty source snapshot recorded beside this file.
- No application or test source was edited during review.

## Findings

### R-01 [P1] Refresh-token consumption is not atomic

Location: `spring-api/src/main/java/koh/portfolio/springapi/application/auth/service/RefreshTokenService.java:62` and `infrastructure/persistence/auth/RefreshTokenJpaRepository.java:20` under the same Java package root.

Two requests can read the same unrevoked token before either commits. The conditional revoke query returns void, so the second caller continues even when its update affects zero rows. If successor issuance occurs in different seconds, both successors can remain active. Likewise, a refresh that read the token before logout can continue after logout has revoked it. This violates single-use rotation and the one-active-refresh-token policy.

Fix direction: atomically consume the old token and require exactly one affected row, or serialize consumption with an appropriate lock. Also enforce the per-user active-session invariant across login and refresh paths. Reject the losing request.

Verification needed: controlled PostgreSQL transactions for concurrent refresh and refresh-vs-logout. Repository-mock tests do not exercise the race. This race was established by source/SQL inspection, not a live database concurrency run.

### R-02 [P1] Refresh tokens issued in the same second can be identical

Location: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/security/jwt/JwtProvider.java:49`.

Refresh claims contain only subject and issued/expiry timestamps. There is no nonce or token ID. JWT NumericDate serialization uses seconds, so repeated issuance for one user in the same second produces the same signed token. `V2__create_refresh_tokens.sql:4` makes token globally UNIQUE; revoked rows remain. Two quick logins or login followed immediately by refresh can therefore fail the insert with a unique-constraint violation, returning an internal error instead of a valid new session.

Fix direction: add a unique issuance identifier, or use a suitable random opaque refresh token; preserve the rotation and storage policy.

Verification needed: exercise the real token provider with same-second issuance and persist both rotated records. Existing tests mock distinct token strings or only validate a single generated token. No executable token-collision reproduction is claimed here.

### R-03 [P1] Concurrent requests can double-book one schedule

Location: `spring-api/src/main/java/koh/portfolio/springapi/application/reservation/service/CreateReservationService.java:45`.

Both requests can observe `existsActiveByScheduleId == false`, then insert REQUESTED reservations. `V7__create_reservations.sql:30` supplies a normal index, not an active-reservation uniqueness constraint. The transaction annotation does not make this check-then-insert sequence exclusive.

Fix direction: enforce at most one REQUESTED/APPROVED reservation per schedule at the database boundary, then map a collision to RESERVATION-001. Consider all paths that change active status, not only creation.

Verification needed: two concurrent real-database requests for one schedule must produce one success and one conflict. Existing mock tests only simulate a pre-existing duplicate.

### R-04 [P1] Concurrent transitions overwrite terminal reservation states

Location: `spring-api/src/main/java/koh/portfolio/springapi/application/reservation/service/RejectReservationService.java:17`; shared persistence path: `infrastructure/persistence/reservation/ReservationPersistenceAdapter.java:19` under the same Java package root.

Approve and reject can both load REQUESTED, both pass domain validation, and then save in sequence. If reject saves first, approve overwrites REJECTED with APPROVED. Similarly, complete and cancel can both load APPROVED and produce a forbidden terminal-state overwrite. The entity has no optimistic version and the update does not compare the expected original status.

Fix direction: enforce the expected source status in an atomic update, or use optimistic/pessimistic concurrency control with conflict handling. The new reject service also lacks a service transaction, but adding that annotation alone does not solve this race.

Verification needed: coordinated approve-vs-reject and complete-vs-cancel requests, asserting only one success. Current transition tests exercise sequential mocked states.

### R-05 [P1] Suspended hospitals still accept new reservations

Location: `spring-api/src/main/java/koh/portfolio/springapi/application/reservation/service/CreateReservationService.java:34`.

The service checks the schedule's hospital ID and BLOCKED status but never loads the hospital's status. After suspension, a caller can submit a previously obtained available schedule ID and create a reservation. State design explicitly prohibits new bookings for SUSPENDED hospitals. Hiding a hospital in search does not enforce that rule at the API boundary.

Fix direction: validate hospital availability when creating a reservation and define its consistency with concurrent suspension. Add a suspended-hospital scenario.

### R-06 [P2] Deleting a pet breaks its reservation history and the entire list

Location: `spring-api/src/main/java/koh/portfolio/springapi/application/reservation/service/GetReservationService.java:80`.

Reservation rendering uses the active-only pet lookup. After DeletePetService logically deletes a reserved pet, that lookup returns empty and throws RESERVATION-004. List mapping aborts on that one reservation, so even reservations for other pets disappear behind a 403 response. The existing reservation and its ownership have not disappeared.

Fix direction: use an appropriate historical association lookup or stored reservation display snapshot while maintaining authorization. Test a deleted pet with an existing reservation and another unaffected reservation.

### R-07 [P2] A refresh token used as Bearer causes an uncaught filter exception

Location: `spring-api/src/main/java/koh/portfolio/springapi/infrastructure/security/jwt/JwtAuthenticationFilter.java:34`.

Generic token validation accepts a correctly signed, unexpired refresh token. Refresh tokens have no role claim, so constructing SimpleGrantedAuthority with that value throws IllegalArgumentException in the filter. This occurs before the MVC exception handler and does not follow the intended authentication-error contract.

Fix direction: explicitly validate access-token purpose and mandatory claims at the filter boundary, and return the common 401 authentication response on malformed/wrong-purpose tokens.

Verification needed: a real security-filter-chain test using a refresh token as Authorization Bearer. Existing MVC tests disable the filters.

### R-08 [P2] Requests queued behind a failed frontend refresh never settle

Location: `frontend/src/api/client.ts:57` and `frontend/src/api/client.ts:86`.

When several requests receive 401, later requests queue only a resolve callback. If refresh fails, the catch block clears the queue without rejecting those promises. The first request rejects, but the others remain pending indefinitely, leaving awaiting callers and loading state unfinished.

Fix direction: use one shared refresh promise or queue both resolve and reject callbacks and settle every waiter on all outcomes. Ensure retry limits apply to queued requests as well.

Executed reproduction: transpiled the actual client.ts in memory with the existing TypeScript dependency, replaced only import.meta.env for Node, and mocked Axios/store transport. Started two 401 handlers, then rejected refresh. Output: `first request: rejected`; `queued request: STILL PENDING after refresh failure`. This is an isolated client-logic reproduction, not browser E2E.

## Architecture assessment

The reviewed domain packages contain no JPA imports. Controller / UseCase / Service / repository / adapter separation and MapStruct boundaries are largely present. Domain-level transition validation alone cannot enforce consistency across transactions, as R-03/R-04 demonstrate.

LoginService and RefreshTokenService import the concrete infrastructure JwtProvider directly. A token-issuance port would restore the intended inward dependency direction and make clock/token behavior easier to test. This is a structural improvement, distinct from the actionable runtime findings above.

Reservation history rendering performs per-reservation associated lookups. Evaluate query count and pagination as data grows. This was not performance-benchmarked and is not presented as a measured regression.

## Test results and gaps

| Check | Result |
|---|---|
| `./gradlew.bat clean test` from spring-api | Passed: 140 tests, 0 failures/errors/skips, exit 0 |
| Initial sandboxed Gradle attempt | Failed to write C:\.gradle wrapper lock; successful run used authorized cache access |
| `npm run build` from frontend | Passed TypeScript and Vite production build, exit 0 |
| Isolated client.ts refresh-failure scenario | Reproduced R-08; source not modified |
| PostgreSQL concurrency / migrations | Not executed |
| Browser/mobile E2E | Not executed |

Current tests include an H2 context smoke test with ddl-auto=create-drop and Flyway disabled; the root policy's statement that H2 has not been introduced is stale. This smoke test does not validate PostgreSQL migrations or production constraints. Controller tests disabling filters cannot establish JWT filter behavior. There is no frontend test script in package.json. Service mocks hide the token uniqueness and transaction races above.

Prioritize real-database concurrency/constraint tests for R-01 through R-04, a full security-chain test for R-07, and client async tests for R-08. Keep the existing fast unit tests as well.

The build emits a MapStruct warning for the Hospital `suspend` target property. No resulting field corruption was established in this review; investigate it separately rather than treating the warning alone as a confirmed bug.

## Scope limits and next action

Known missing hospital-affiliation authorization and Notification integration remain risks but were explicitly documented as incomplete work, so they are not counted among the eight findings. Unimplemented feature screens and Django's Dockerfile are also excluded from completed-feature acceptance.

Address R-01 through R-05 first, then the remaining correctness issues. Create a new handoff revision after fixes; this review does not approve future revisions or merge/deploy anything. No claim of exhaustive vulnerability coverage is made.
