# Handoff 001: 2026-10-26-security-portability

- Author / implementer: Claude Code (design/edits: main session; regression runs: two Sonnet subagents)
- Publication: ready
- Timestamp: see TASK.md "Updated at"
- Task specification: TASK.md
- Recipient: Codex (model gpt-6.1-sol)
- Requested action: review only
- Source base SHA: `3241f26` (`feat/analysis/analysis-api`, pushed)
- Source head SHA: none — uncommitted working tree on `chore/security-and-portability`
- Diff basis: `git diff 3241f26` plus the untracked files listed in the manifest
- Dirty snapshot manifest: `HANDOFF-001-SHA256.txt` (every changed tracked file and every new file of this task; excludes `docs/report/`, `.vscode/`, `test/` and installer-managed tool files, which are git-ignored or the user's)

## Change summary

| Area | Files | What |
|---|---|---|
| Spring dependencies | `spring-api/build.gradle` | Spring Boot 3.3.13 → 3.5.16 (Framework 6.2.19, Security 6.5.11, Data Commons 3.5.13, Hibernate 6.6.53, Flyway 11.7.2); `ext[...]` overrides Tomcat 10.1.61, Jackson 2.21.8, PostgreSQL JDBC 42.7.14, Log4j API 2.25.5 |
| Spring tests | 16 `presentation/**/**ControllerTest.java` | `@MockBean` → `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`); no other test change |
| Frontend dependencies | `frontend/package.json`, `package-lock.json` | Capacitor 8.4.3, axios 1.20.0, react-router-dom 6.30.6, vite 6.4.4; transitive fixes via `npm audit fix --before=2026-10-20` (tar, brace-expansion, follow-redirects, @xmldom/xmldom, postcss, browserslist, nanoid, source-map-js, …) |
| Images | `frontend/Dockerfile`, `analysis-api/Dockerfile`, `docker-compose.yml` | `node:24.22.1-alpine` (was node 20, EOL), `python:3.12.15-slim`, `postgres:16.15` |
| Security tooling and docs | `scripts/osv-scan.mjs`, docs/09 §22 | OSV scan of Gradle runtimeClasspath + npm lockfile + Python env; before/after table and rationale for the 3 accepted findings |
| Portability (item 3) | `.ai-collab/SETUP.md`, `.ai-collab/scripts/run-codex.sh`, `scripts/notion_prep.py`, `.claude/settings.json`, `skills-lock.json`, `.gitignore`, `CLAUDE.md`, `AGENTS.md`, `README.md` | New-machine setup (tools, plugins via project settings, `npx skills experimental_install`, GSD installer, Codex review prerequisites, verification rules, Notion sync); rules formerly only in Claude auto-memory moved to CLAUDE.md/AGENTS.md; installer-managed files ignored |
| Doc sync | AGENTS.md §1/§8/§9/§10/§11/§12, TODO.md, README.md, docs/09/10/11/12, `.agents/skills/spring-test`, `.claude|.codex/agents/*` | Versions, `@MockitoBean` rule, security section, open practice gap noted |

## Acceptance evidence (2026-10-26)

| ID | Check | Where / command | Outcome |
|---|---|---|---|
| AC-01 | OSV scan | `node scripts/osv-scan.mjs` (main session) | before 31 vulnerable packages of 410 → after 3 of 406 (`evidence/osv-after.json`); remaining: spring-webmvc 6.2.19 (CVE-2026-47884 XsltView, CVE-2026-47890 SSE fragments; fix only in 7.0.9), react-router 6.30.6 (CVE-2026-53666 SSR, CVE-2026-53669 untrusted Link/navigate target; fix only in 7.18), postcss-selector-parser 6.1.5 dev (fix only via Tailwind 4) — rationale docs/09 §22-2 |
| AC-02 | JUnit | `spring-api/`: `./gradlew clean test` (Sonnet A) | 382 tests, 0 failures/errors/skips; no `[removal]` warnings |
| AC-03 | API regression, all 52 Spring endpoints | scratch DB `daipetto_reg_api`, Boot 3.5.16 jar on 18081 (Flyway applied 15 migrations), the 134-item runner of the 2026-10-23 unit-test package run from a scratch copy (Sonnet A) | 134/134 OK (`evidence/api-regression-summary.json`); Spring log: no ERROR, no deprecation, only the existing open-in-view and generated-password startup warnings |
| AC-04 | Analysis | pytest (Sonnet A); E2E on `daipetto_reg_an`, analysis container built from `python:3.12.15-slim`, Spring on 18083 (Sonnet A) | 38 passed; 17/17 OK (`evidence/analysis-e2e-result.json`) |
| AC-05 | Frontend | `npm run build` (Sonnet A); browser regression on Vite 5174 + Spring 18082 + seeded scratch DB (Sonnet B; the scratch backend needed `--cors.allowed-origins=http://localhost:5174`, now in SETUP §5) | build OK; S-01..S-16 all OK — login and reload token restore, pet list/detail/edit, health record add, vaccination complete, hospital keyword/area search (suspended hidden), 3-step reservation (201), logout + guard redirect, vet approve, vet business hours/slots, admin user search, cancel + notification read, role guard, register duplicate-email message (409); no app console errors, no 5xx |
| AC-06 | Images | `docker compose build analysis-api frontend` (Sonnet A) | both built; container reports Python 3.12.15 |
| AC-07 | Portability | file review | see change summary; `npx skills experimental_install` confirmed in `skills@1.7.4 --help`; `get-shit-done-cc@1.18.0` exists on npm; runner locates `codex-companion.mjs` without `ls` |

Scratch databases were dropped and ports freed by both subagents; DB `daipetto` and port 8080 were not touched.

## Limitations

- Spring Boot 3.x is past OSS support; future Spring CVEs need the 4.x migration (TODO.md). The three remaining findings are accepted as unreachable, not fixed.
- Local Python is 3.12.13 (newest offered by the installed uv); Docker uses 3.12.15. Local Node (24.17.0) and the user's PostgreSQL container (16.9) are updated by the user (TODO.md).
- PostgreSQL, browser and Docker results are implementer-reported. The reviewer sandbox has no network: OSV and npm audit cannot be rerun there; `spring-api/.gradle/review-home` was warmed with the new dependencies for `--offline`.
- `.codex/config.toml` keeps a Windows `npx.cmd` path (documented in SETUP §2).

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. Run JUnit with `GRADLE_USER_HOME=spring-api/.gradle/review-home gradlew.bat test --offline --rerun-tasks` (compare with 382), pytest with `analysis-api/.review-venv/Scripts/python.exe -m pytest -q` from `analysis-api/` (38), and `npm.cmd run build` in `frontend/` (offline; `node_modules` is present). Check: the version overrides really take effect (resolved versions), the `@MockitoBean` conversion keeps test semantics, the accepted-finding rationale against the code (no XSLT/SSE, no SSR, navigation targets), the SETUP/CLAUDE/AGENTS instructions for correctness and secrets (no credentials committed), the `.gitignore` patterns (no tracked project file ignored), and doc/version consistency. Write `REVIEW-001.md`.
