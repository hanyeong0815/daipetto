# Handoff 001: 2026-10-25-analysis-api

- Author / implementer: Claude Code (design and code: main session Opus 5.5; full test runs, image builds and the E2E run: Sonnet 5.5 subagents, per user instruction)
- Publication: ready
- Timestamp: 2026-10-25T03:25+09:00
- Task specification: TASK.md
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: review only
- Source base SHA: `3ee6e6b` (`test/unit-test`, pushed; its PR to `develop` is pending, by the user)
- Source head SHA: none — uncommitted working tree on `feat/analysis/analysis-api`
- Diff basis: `git diff 3ee6e6b` (tracked changes and renames) plus the untracked paths listed in the manifest
- Checkout/branch and dirty state: `E:\workspace\daipetto`, `feat/analysis/analysis-api`, dirty
- Dirty snapshot manifest: `HANDOFF-001-SHA256.txt` (every changed tracked file, every new file of this task; excludes `.venv/`, `__pycache__/`, `docs/report/` and the user's `.claude/`/`.codex/`/`.agents/` tool installs)

## Change summary

| Area | Files | What |
|---|---|---|
| Analysis API (new, FastAPI) | `analysis-api/app/{main,config,db,security,errors,schemas,repository,services,routers}.py`, `requirements*.txt`, `.env.example`, `.dockerignore`, `Dockerfile` (moved from `django-api/`) | Internal endpoints `/internal/v1/health` and `/internal/v1/analysis/pets/{id}/weight|health-score|vaccination/next`; `X-Internal-Api-Key` checked with `hmac.compare_digest` before any DB access (unset key denies all); common envelope incl. 422→400 VALIDATION-001, 404/405, 500 without details; OpenAPI routes disabled; per-request psycopg connection with `default_transaction_read_only=on`; SQL with bound parameters only; services are pure functions; "today" in Asia/Tokyo (`tzdata`) |
| Spring relay (new) | `domain/analysis/**`, `application/analysis/**`, `infrastructure/client/analysis/**`, `presentation/analysis/AnalysisController.java`, `application.yml`, `.env.example` | `GET /api/v1/analysis/pets/{petId}/weight?from&to`, `/health-score`, `/vaccination/next`. Owner check (`AnalysisPetChecker`, deleted/missing/other → 403 ANALYSIS-001), `from > to` → 400 ANALYSIS-002, every upstream failure (non-2xx, I/O, timeout 2 s/5 s, unreadable or `success:false` body) → 503 ANALYSIS-003. Settings `analysis-api.url` / `analysis-api.internal-api-key` (env `ANALYSIS_API_URL` / `INTERNAL_API_KEY`) |
| Compose | `docker-compose.yml` | service `django-api` → `analysis-api`; Spring env `DJANGO_API_URL` → `ANALYSIS_API_URL`; `DJANGO_DEBUG` removed |
| Docs | docs 01/03/04/05/07/09/10/11/12/13, README.md, TODO.md, AGENTS.md, `test/README.md`, `.gitignore` comment, agent definitions renamed to `analysis-api-dev` | Spec of the four analysis APIs (docs/07 §7, three implemented, 7-3 practice gap), routing decision (docs/04 §8, docs/09 §4-6), FastAPI rationale (docs/04 §5-2, docs/13 §3-4), environment (docs/10 §6, §9), tests (docs/11 §6-10); README rewritten to the current state |

Settled constraints: the frontend never calls the analysis API; the analysis API never writes and creates no tables; practice gap = symptoms/summary on both sides (no route, no use case); no frontend change in this round.

## Acceptance evidence

| Criterion | Test/check | Working directory and command | Environment | Outcome | Evidence location |
|---|---|---|---|---|---|
| AC-01, AC-02 | pytest `test_common.py` | `analysis-api/`: `.venv/Scripts/python.exe -m pytest -q` | Python 3.12.10, fake DB connection | 37 passed (whole suite) | — |
| AC-04, AC-06 | pytest `test_services.py`, `test_analysis_routes.py` | same | same | included above | — |
| AC-05 (Spring part) | JUnit `AnalysisServiceTest`, `AnalysisControllerTest`, `AnalysisApiClientTest` | `spring-api/`: `./gradlew clean test --offline` (Sonnet subagent) | JDK 17, H2/mocks | 367 tests, 0 failures/errors/skips | `spring-api/build/test-results/test/` |
| AC-03 | container smoke | `docker compose run` on scratch DB `daipetto_fa_prep` (Flyway V15) (Sonnet subagent) | PostgreSQL 16.9 | used columns exist; `CREATE TABLE` rejected with `ReadOnlySqlTransaction` | TASK.md evidence |
| AC-04..AC-06 end to end | `scripts/analysis-e2e.mjs` | repo root, Spring jar 18083 + analysis container 18084 on scratch DB `daipetto_an_e2e` (Sonnet subagent) | as above | 17/17 OK; scratch DB dropped | `evidence/e2e-result.json` |
| Image | `docker compose build analysis-api` | repo root | Docker | built; `/app` has no `.venv`, `.env`, `tests` | — |

## Limitations

- PostgreSQL, Docker and E2E results are implementer-reported (reviewer likely has no Docker).
- The health-score is a heuristic reference value (documented as 参考値); clinical validity is out of scope.
- Weight values lose trailing zeros in JSON (`4.3`, not `4.30`); documented in docs/07 §7.
- Per-request DB connections (no pool), marked with a `ponytail:` comment.
- `test/` (the accepted unit-test package) does not include the analysis APIs; `test/README.md` points to this task.
- Notion is not yet synced; planned after acceptance (user request).

## Documentation

See the change summary. AGENTS.md §1/§5/§8/§9/§10, TODO.md analysis section (practice gap listed), README.md rewritten.

## Response to previous review

| Finding ID | Disposition | Evidence | Remaining action |
|---|---|---|---|
| None for first round | N/A | N/A | N/A |

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. Run `python -m pytest` in `analysis-api/` (create a venv with `uv venv --python 3.12.10 .venv` and `uv pip install --python .venv -r requirements-dev.txt` if `.venv` is unusable for you) and JUnit in `spring-api/` with `GRADLE_USER_HOME=spring-api/.gradle/review-home --offline`. Check the services against docs/07 §7, the security boundary (key check before DB, unset key, no docs routes, read-only connection, no SQL string building from input), the Spring error mapping, the practice gap, and doc consistency. Write `REVIEW-001.md`.
