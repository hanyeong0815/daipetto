# Handoff 001: 2026-10-23-unit-test

- Author: Claude Code
- Publication: ready
- Timestamp: 2026-10-23T01:00+09:00
- Task specification: TASK.md
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: review of the test design and evidence so far (round 1). Screen tests (UT-14) are still in progress and out of scope for this round.
- Source base SHA: `7ee0db6` (develop) on branch `test/unit-test`
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD` plus untracked `test/`
- Dirty snapshot: `HANDOFF-001-SHA256.txt` (changed tracked files + every file under `test/` except `test/evidence/coverage/html/`)

## What was done

1. JaCoCo added to `spring-api/build.gradle` (`test` is finalized by `jacocoTestReport`, XML + HTML).
2. API unit tests: `test/scripts/api-cases.mjs` (125 items, expectations taken from docs/07), `lib.mjs` (HTTP/DB helpers, evidence writer, password/token masking), `run-api-unit-tests.mjs` (fixtures + execution). Run against a backend on a fresh scratch PostgreSQL DB `daipetto_ut`, port 18081.
3. `build-spec.mjs` copies JUnit XML and JaCoCo into `test/evidence/` and generates `test/spec/UT-01..13_*.md` and `test/RESULTS.md`.
4. First run: 112/123 OK. Defects found and fixed (see `test/DEFECTS.md`):
   - D-003..D-005 (implementation): unknown URL / unsupported method / unsupported Content-Type returned 500. Fixed in `GlobalExceptionHandler` with COMMON-001/002/003 (404/405/415). Regression tests were added in `HospitalControllerTest` and `PetControllerTest`.
   - D-001, D-002, D-006 (spec): docs/07 §4-1 USER-002 → VALIDATION-001; §3-3 logout contract was stale; petType allowed values were undocumented. docs/07, docs/06 and AGENTS §5 were updated.
   - T-001..T-004: test-side mistakes, listed separately.
5. Final run so far: API 125/125 OK, JUnit 321/321 pass, coverage line 86.3% / branch 74.6%.

## Review requests

1. **Coverage of the test items (main request).** Enumerate every endpoint in `spring-api/src/main/java/**/presentation/**/*Controller.java` and both schedulers. Also take every error code listed per API in docs/07 (and branches visible in the services). List anything not exercised by either an API item (`test/scripts/api-cases.mjs`, `test/spec/*.md` section 1) or a JUnit test (`test/spec/*.md` section 2). Give each gap a proposed item and expected result.
2. Expected results that contradict docs/07 or the code (wrong expectations, weak checks).
3. Evidence integrity:
   - Spot-check `test/evidence/api/*.json` against the spec tables (verdict, request/response, masking of passwords/tokens).
   - Check that `test/evidence/junit` matches the spec section 2 counts.
   - Rerun `./gradlew test` in `spring-api/` with `GRADLE_USER_HOME=spring-api/.gradle/review-home --offline` and compare the count (321).
4. DEFECTS.md: are the cause and disposition of D-001..D-006 sound? (Fixing docs vs code.)

Docker is probably unavailable to you; mark PostgreSQL runs reported-only.

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. Write `REVIEW-001.md` in this directory using `.ai-collab/templates/REVIEW.md`, with stable finding IDs (G-xx for coverage gaps, R-xx for other findings) and a verdict. Do not modify other files. Keep `spring-api/.gradle/review-home`.
