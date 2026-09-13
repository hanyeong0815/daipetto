# Codex review procedure

Use the [shared review template](../.ai-collab/templates/REVIEW.md). The protocol defines lifecycle and ownership; do not create another status convention here.

## 1. Establish the target

Check base/head SHA, diff basis, dirty snapshot if any, acceptance criteria, and excluded practice gaps. Read changed files and their callers, security boundaries, persistence queries, configuration, and tests as needed. A changed head invalidates affected prior conclusions.

## 2. Select the relevant checks

| Change | Focus | Existing docs by prefix |
|---|---|---|
| Feature behavior | Observable acceptance, actor permissions, excluded scope | 02, 03 |
| Architecture | Domain isolation, UseCase/Port/Adapter boundaries, MapStruct | 04 |
| UI | API mismatch, loading/errors, accessibility, stale state, role guards | 05, 07 |
| Database | Constraints, deletion, migration numbering across relevant refs, transactions | 06 |
| API | Request/response, validation, ErrorCode and HTTP status | 07 |
| State changes | Permitted actor and transition, concurrent requests, time boundaries | 08 |
| Auth | Long principal, ownership, refresh rotation/replay, revocation and retry races | 09 |
| Build/runtime | Actual toolchain, platform-specific base URL, configuration | 10 |
| Tests and incidents | Acceptance coverage, boundaries, regression evidence, known pitfalls | 11, 12 |

Root AGENTS.md supplies project-specific rules. Inspect actual files before assuming a domain exists or a migration number is available. Review hospital affiliation gaps as documented limitations unless the current request changes that policy. Do not implement the user's practice exercises.

## 3. Verify

- Spring changes: from spring-api, use ./gradlew.bat clean test on Windows or ./gradlew clean test on Unix when available.
- Frontend changes: from frontend, use npm run build; run npm run lint only with actual configuration/dependencies available. Do not invent an npm test script.
- Documentation-only work: check file contents, local links, and consistency.
- For every executed command record the directory, revision, exit code, and actual result. Distinguish reported results from personal execution.
- Unit tests with mocked repositories do not validate real SQL or concurrency. Note the gap or run an appropriate existing integration check; do not install a new test stack merely to make a checklist green.

## 4. Report and re-review

Use stable finding IDs, severity, exact file/line, trigger, effect, evidence, and a meaningful regression check. Prefer actionable correctness issues over style preferences. If no actionable defect is found, state the inspected scope and unverified areas.

Re-review fixes against a new handoff round. Preserve previous findings and explain resolutions or withdrawals. Do not repeat full validation without new changes or unresolved concerns. Acceptance is tied to one snapshot; it does not merge or deploy code.
