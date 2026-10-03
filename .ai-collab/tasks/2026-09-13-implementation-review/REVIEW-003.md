# Review 003: HANDOFF-003

- Reviewer: Codex.
- Date: 2026-10-03, Asia/Seoul.
- Publication: final.
- Verdict: INCOMPLETE for full handoff acceptance; no actionable new code defect found in the reviewed changes.
- Consumed handoff: [HANDOFF-003.md](HANDOFF-003.md), still marked draft. The user's explicit request authorized reviewing it now; its publication state was not changed.
- Source HEAD: 5bbf885295b3c584e6e7d4cbcd524a083c677b54 plus HANDOFF-003 working-tree changes.
- Identity: all 47 manifest entries matched both before and after review.
- Application/test source changes by reviewer: none.

## R-01 disposition

The source-level fix addresses the previously reproduced race. LoginService:52, RefreshTokenService:64 and LogoutService:22 acquire the same user-row lock before modifying refresh tokens, inside their existing service transactions. UserJpaRepository:15 uses PESSIMISTIC_WRITE for that lookup; UserPersistenceAdapter delegates through the repository port. The transaction retains the database lock even when a later modifying query clears the persistence context.

With PostgreSQL's default READ COMMITTED behavior, a waiting login/logout obtains the user lock after refresh commits. Its subsequent bulk revoke is a new statement and can therefore see the newly committed successor. Conversely, if logout/login wins first, the delayed refresh's conditional revoke affects zero rows and prevents successor issuance. Concurrent logins now serialize their revoke/insert sequences.

The earlier token lookup before locking does not restore the old replay issue: the conditional database update, rather than the previously read domain object's revoked flag, is the final consumption gate.

**Assessment:** R-01 is resolved by code inspection and independently executed SQL-mechanism checks. Full Spring HTTP concurrency evidence remains pending; do not label that layer verified.

## Independently executed evidence

| Check | Result |
|---|---|
| `./gradlew.bat clean test`, spring-api | Exit 0; 161 tests, 0 failures/errors/skips |
| Manifest validation | 47/47 current files match HANDOFF-003-SHA256.txt |
| Refresh then concurrent logout | Second operation waited 2441 ms; final active tokens = 0 |
| Refresh then concurrent login | Second operation waited 2558 ms; final active tokens = 1 |
| Two concurrent refresh operations | Second operation waited 2468 ms; its conditional successor INSERT affected 0 rows; final active tokens = 1 |
| Two concurrent login operations | Second operation waited 2392 ms; final active tokens = 1 |

SQL experiments ran on PostgreSQL 16.9 in scratch database `codex_r01_lock_review_20261003`, successfully dropped afterward. The existing development database was not modified. The scratch schema contained a users row and a minimal refresh_tokens table with token/user_id/revoked, not the full production schema. Each case started with exactly one active old token.

Both transactions acquired `SELECT id FROM users WHERE id=1 FOR UPDATE` before token writes. The first transaction revoked the initial token, paused for three seconds, inserted successor A, then committed. The second began during that pause and either bulk-revoked, bulk-revoked plus inserted B, or conditionally consumed the original token and inserted only if consumption succeeded. For the single-active-row login seed, the first transaction's targeted revoke is equivalent to revoking all active tokens in that seeded state; this is a mechanism test, not execution of LoginService itself.

The new mock InOrder checks establish application call order. The H2 adapter test executes the locking query. Together with the PostgreSQL checks these provide substantially stronger evidence than the draft handoff had, but they are not concurrent requests through the complete Spring transaction/security stack.

## Other changes

- The reservation adapter now translates the named active-schedule constraint and propagates unrelated integrity violations. Both mapping and propagation regression tests pass. No new blocking issue was identified.
- R-02 through R-08 retain the preceding review's dispositions; this round was focused on the latest changes, not a fresh whole-system audit.
- The known MapStruct `suspend` warning persists; no new field corruption was found.
- Frontend code is unchanged from the preceding validated snapshot, so no repeat frontend build was needed.

## What remains before final acceptance

HANDOFF-003 explicitly lists SQL plus HTTP scenarios E1–E5, including proving that each real operation waits while the user row is held. This reviewer executed the SQL interleavings above but did not run the claimed `r01-sql.sh` / `r01-http.mjs` scripts or start the backend on port 18080. Those scripts were not found under the shared task folder, and the handoff does not provide a reproducible repository path to them.

Claude should provide the scripts or exact repeatable commands, execute the pending full-stack checks against an isolated database, and report their actual outcomes. Docker was available on 2026-10-03, unlike the environment described in the draft. Preserve the consumed draft and publish a new ready handoff round with the evidence; update the source manifest if any source changes occur.

This is a verification gap, not a newly demonstrated P1 defect. No additional code correction is requested on the evidence reviewed here. TASK.md must not be marked VERIFIED until the remaining agreed acceptance checks are resolved. No merge or deployment was performed.
