# Task: dependency security update, regression check, portable agent setup

- Task ID: 2026-10-26-security-portability
- Coordinator (single writer): Claude Code
- Implementer: Claude Code (design and edits: main session; test execution and regression runs: Sonnet subagents, per user instruction)
- Reviewer: Codex, model `gpt-6.1-sol`
- Mode: implementation-and-review
- Status: CLOSED (accepted in REVIEW-002; committed and pushed on `chore/security-and-portability`)
- Updated at: 2026-10-26T00:52+09:00
- User requests (2026-10-25/26, Korean, paraphrased): (1) remove the `test/` folder from every branch (not for GitHub); (2) check whether recently reported vulnerabilities are addressed and fix them if not; (3) organize the md files so another computer gives the same agent performance; (4) while doing so, test with Codex and subagents that the vulnerability fixes break no other feature.
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `chore/security-and-portability` (from `feat/analysis/analysis-api` at `3241f26`; PRs for `test/unit-test` and `feat/analysis/analysis-api` are still pending, by the user)
- Item (1) is already delivered outside this branch: `test/unit-test` `59fc015`, merged into `feat/analysis/analysis-api` `3241f26` (both pushed). The folder stays only as an ignored local copy.

## Requirements

| ID | Check | Expected |
|---|---|---|
| AC-01 | OSV scan of all resolved dependencies (`node scripts/osv-scan.mjs`) | Every finding with an OSS fix in a compatible line is fixed; each remaining finding has a reachability rationale (docs/09 §22-2) |
| AC-02 | Spring Boot 3.5.16 + overrides compile and pass the existing tests | JUnit all green (382), no `@MockBean` removal warnings |
| AC-03 | Regression of every Spring endpoint against a scratch PostgreSQL | The 134 API unit-test items from the 2026-10-23 package all OK |
| AC-04 | Analysis API path (Spring → FastAPI → PostgreSQL) | pytest 38; E2E 17/17 |
| AC-05 | Frontend after axios/react-router/Capacitor/Vite updates | build OK; browser regression of the main screens (login/session restore, pets, health records, vaccinations, hospital search, reservation, admin, guards, register error message) without console errors or 5xx |
| AC-06 | Docker images build with the new bases | `analysis-api` (python 3.12.15) and `frontend` (node 24.22.1) build |
| AC-07 | Portable agent setup | A new machine can restore plugins, skills, review prerequisites and rules from committed files (`.ai-collab/SETUP.md`, `.claude/settings.json`, `skills-lock.json`, `CLAUDE.md`, `AGENTS.md`); installer-managed files are ignored |

## Decisions

| Decision | Source |
|---|---|
| Spring Boot 3.5.16 (last OSS 3.x) plus component overrides now; Spring Boot 4.x migration as a later task | User choice 2026-10-25 |
| npm fixes resolved with `--before=2026-10-20`; Vite 6.4.4 and Capacitor 8.4.3 instead of the newest patch (versions < 3 days old avoided) | Implementer (supply-chain caution) |
| Remaining 3 findings accepted as unreachable (docs/09 §22-2) | Implementer; reviewer to confirm |
| `@MockBean` → `@MockitoBean` in all tests; AGENTS/skills/agent definitions updated | Implementer (Boot 3.4+ deprecation; Boot 4 removal) |
| Local Python 3.12.13 (newest uv 0.11.21 offers); Docker 3.12.15 | Environment limit |
| Rules formerly only in Claude auto-memory moved to CLAUDE.md (delegation/models, Git workflow) and AGENTS.md (practice-gap nuance) | Item (3) |

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001: R-01..R-06)
- Next actor and action: the user opens the PRs in order: test/unit-test, feat/analysis/analysis-api, chore/security-and-portability
- Latest final review: REVIEW-002.md (ACCEPTED; REVIEW-001 R-01..R-06 resolved)

## Closure

- Delivered source revision: the commit on `chore/security-and-portability` that contains this record (parent `3241f26`)
- Review: accepted (REVIEW-001 CHANGES_REQUESTED → HANDOFF-002 → REVIEW-002 ACCEPTED)
- Verification evidence: reviewer-executed JUnit 382 (`--rerun-tasks`), pytest 38, frontend build, `test-run-codex.sh`, OSV fail-fast; implementer-reported API regression 134/134, E2E 17/17, browser S-01..S-16, image builds, OSV 31 → 3
- Remaining: Spring Boot 4.x, React Router 7, Tailwind 4 migrations; user's local Node and PostgreSQL container updates (TODO.md)
- Integration state: committed and pushed; PR by the user
- Notion (2026-10-26): pages 07, 09, 10, 11, 12 updated with targeted `update_content`/`insert_content` (versions, docs/09 §22, docs/11 unit-test record, docs/07 notes); no warnings. `.gitattributes` (`*.sh text eol=lf`) was added after REVIEW-002 so the shell scripts survive `core.autocrlf=true` checkouts.
