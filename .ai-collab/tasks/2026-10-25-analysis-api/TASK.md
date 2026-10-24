# Task: Analysis API (FastAPI, 4 endpoints) behind a Spring relay

- Task ID: 2026-10-25-analysis-api
- Coordinator (single writer): Claude Code
- Implementer: Claude Code (test execution by Sonnet subagents, per the user's model-split instruction)
- Reviewer: Codex, model `gpt-6.1-sol` unless heavy reasoning is needed
- Mode: implementation-and-review
- Status: CLOSED (accepted in REVIEW-002; committed and pushed on `feat/analysis/analysis-api`)
- Updated at: 2026-10-25T03:42+09:00
- User requests (2026-10-25, Korean, paraphrased): (1) "If Spring work including the frontend wiring is done, open the PR to develop and prepare the Django project on a new branch." (2) "For APIs this small, is Django needed? Switch to the framework that fits the situation better; narrow the candidates and let me choose." → chose FastAPI and the folder `analysis-api/`. (3) "Proceed with remaining item 2 (the spec draft → implementation); tidy README and the docs; reflect them in Notion with the current docs as the base." The `test/unit-test` branch is kept and gets its own PR (user choice).
- Source checkout, branch, initial HEAD: `E:\workspace\daipetto`, `feat/analysis/analysis-api` (new, from `test/unit-test` at `3ee6e6b`; renamed from `feat/django/analysis-api` before any push; the `test/unit-test` PR to `develop` is pending, by the user)
- Owned files: `analysis-api/**` (was `django-api/`), `docker-compose.yml` analysis service, Spring relay for the analysis APIs (phase 2), docs 01/03/04/05/07/09/10/11/12/13, README.md, TODO.md, AGENTS.md §1/§5/§9/§10, `.gitignore` comment, both `analysis-api-dev` agent definitions (renamed from `django-api-dev`).

## Requirements

Spec: docs/07 §7 (draft), docs/04 §5-2/§8/§12, docs/09 §4-6, docs/10 §6, docs/11 §6-10.

| ID | Input/action | Expected observable result | Planned verification |
|---|---|---|---|
| AC-01 | Internal API with a valid / missing / wrong / non-ASCII / unset `X-Internal-Api-Key` | 200 / 401 INTERNAL-001 (unset key denies all); no DB access before the key check | pytest (done) |
| AC-02 | Unknown URL, wrong method, unexpected exception, `/docs` `/openapi.json` | 404 COMMON-001, 405 COMMON-002, 500 SERVER-001 without details, docs routes 404 | pytest + container run (done) |
| AC-03 | DB access | read-only enforced by PostgreSQL; the columns used by the analysis exist in the Flyway schema | scratch PostgreSQL smoke (done) |
| AC-04 | 7-1 weight, 7-2 health-score, 7-4 vaccination/next through Spring | data per docs/07 rules (daily value, penalties/grades, SCHEDULED > COMPLETED+1y) | pytest service tests + scratch-DB API run (phase 2) |
| AC-05 | Other user's / deleted / missing pet; `from` > `to`; bad date; analysis API down | 403 ANALYSIS-001; 400 ANALYSIS-002; 400 VALIDATION-001; 503 ANALYSIS-003 | Spring tests + API run (phase 2) |
| AC-06 | No data | empty arrays, `summary: null`, `NO_DATA` | tests (phase 2) |

## Exclusions and constraints

- Practice gap: 7-3 symptoms/summary (Python router/schema/service/repository and the Spring relay route) stays unimplemented for the user (TODO.md, docs/07 heading 「未実装・練習用」).
- The analysis API never writes to the DB and creates no tables; tables come from Spring Flyway.
- Frontend charts are out of scope until the user asks.
- Never use DB `daipetto` or port 8080 for verification; scratch DBs only.

## Decisions

| Decision | Source |
|---|---|
| Frontend → Spring (JWT, ownership) → analysis API internal endpoints (`/internal/v1`, `X-Internal-Api-Key`); no JWT and no CORS in the analysis API | User choice 2026-10-25 (docs/04 §8 and docs/10 §13-2 disagreed; compose already wired an internal key) |
| Four endpoints from TODO.md (replace the single docs/07 `summary`) | User choice 2026-10-25 |
| Health-score formula and the 1-year vaccination interval as drafted in docs/07 §7 | User: "proceed with item 2" (2026-10-25) |
| Spring returns typed domain records (`WeightAnalysis`, `HealthScore`, `VaccinationForecast`) deserialized from the analysis API; three UseCase services share `AnalysisPetChecker` (UseCases all use `execute`, so same-signature UseCases cannot share one class) | Implementer default |
| Practice gaps apply; chosen gap: symptoms/summary | User choice 2026-10-25 (gap chosen by Claude, can be changed by the user) |
| FastAPI instead of Django REST Framework; TensorFlow/PyTorch excluded (ML libraries, not web frameworks); Flask/Django offered as alternatives | User choice 2026-10-25 after a comparison (docs/04 §5-2, docs/13 §3-4) |
| Folder/service `analysis-api`, Spring env `ANALYSIS_API_URL` | User choice 2026-10-25 |
| psycopg 3 with SQL (no ORM), per-request connection with `default_transaction_read_only=on`; OpenAPI routes disabled (export via `app.openapi()`) | Implementer default |
| pytest + TestClient with httpx2 (Starlette 1.10 deprecates httpx for the test client); DB replaced by a fake connection in tests; SQL checked on a scratch PostgreSQL | Implementer default |

## Evidence (phase 1, 2026-10-25)

| Check | Where / command | Outcome |
|---|---|---|
| Unit tests | `analysis-api/`: `.venv/Scripts/python.exe -m pytest` (Claude Code) | 9 passed |
| Image | `docker compose build analysis-api` (Sonnet subagent) | built on `python:3.12.10-slim` with the pinned packages; `/app` has no `.venv`, `.env` or `tests` (`.dockerignore`) |
| Runtime | DB `daipetto_fa_prep` with Flyway V1..V15 (Spring jar on 18083); `docker compose run -d -p 18084:8000` with the real uvicorn CMD (Sonnet subagent) | health 200 with key; 401 INTERNAL-001 without/with a wrong key; 404 COMMON-001; 405 COMMON-002; `/docs` and `/openapi.json` 404; all `application/json` |
| DB | inside the container | `SELECT` of the used columns of `health_records`/`vaccinations` succeeded; `CREATE TABLE` failed with `ReadOnlySqlTransaction`; no table created; DB dropped, ports free |
| Phase 2 unit tests | `analysis-api/`: `python -m pytest` (Claude Code, then Sonnet subagent) | 37 passed |
| Phase 2 Spring | `spring-api/`: `./gradlew clean test --offline` (Sonnet subagent) | 367 tests, 0 failures/errors/skips (25 new: AnalysisServiceTest 9, AnalysisControllerTest 9, AnalysisApiClientTest 7) |
| Phase 2 E2E | DB `daipetto_an_e2e`; analysis container (`docker compose run`, current source mounted, image rebuilt with tzdata) on 18084; Spring jar on 18083 with `--analysis-api.url/--analysis-api.internal-api-key`; `scripts/analysis-e2e.mjs` (Sonnet subagent) | 17/17 OK (`evidence/e2e-result.json`; no JWT or fixture password in it); DB dropped, ports free |
| Round 2 (REVIEW-001 fixes) | `./gradlew clean test --offline`; pytest; E2E rerun on `daipetto_an_e2e2` (Sonnet subagent) | 382 tests green; 38 passed; E2E 17/17 OK |
| Superseded | Django skeleton (earlier the same day) | removed before any commit. The Codex stop-gate finding on it (DEBUG=True broke the error envelope) does not apply to FastAPI (no debug pages; tested above) |

## Progress references

- Latest published handoff: HANDOFF-002.md (response to REVIEW-001)
- Latest final review: REVIEW-002.md (ACCEPTED; R-01..R-03 resolved)
- Next actor and action: the user opens the PRs (`test/unit-test` first, then this branch)
- Blocker: none
- After acceptance: commit/push this branch; sync the 13 docs to Notion (docs are the base, user request)

## Closure

- Delivered source revision: the commit on `feat/analysis/analysis-api` that contains this record (parent `3ee6e6b`)
- Review: accepted (REVIEW-001 CHANGES_REQUESTED R-01/R-02/R-03 → HANDOFF-002 → REVIEW-002 ACCEPTED)
- Verification evidence: reviewer-executed pytest 38 and JUnit 382 (`--rerun-tasks`); implementer-reported container smoke and E2E 17/17
- Remaining findings and disposition: none open. Accepted limitations: E2E/PostgreSQL implementer-reported, heuristic score, no connection pool
- Integration state: committed and pushed; PR to `develop` by the user

## Notion sync (2026-10-25, user request: docs are the base)

- The 13 pages under "PetCare Platform" (IDs = the hashes in the docs file names; last edited 2026-08-01 before the sync) were overwritten with `replace_content` from the docs (Sonnet subagent; title line removed, no child pages or databases on any page, no warnings).
- Follow-up fixes by the main session with targeted `update_content`: relative `*.md` cross-references on pages 10–13 replaced with native page mentions; broken bold spans (docs/02 BR-001..005 headings, docs/07 §2-7) and an auto-linked version string (docs/10 §5-8) fixed in Notion and in the docs; docs/10 §5-6 stale Flyway range (V1〜V7 → V1〜V15) and package tree fixed in both.
- The doc-only follow-up commit is formatting/wording only; review: skipped (low risk, no behavior or API change).
