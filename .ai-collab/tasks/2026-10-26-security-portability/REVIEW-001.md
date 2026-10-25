# Review 001: 2026-10-26-security-portability

- Author / reviewer: Codex (gpt-6.1-sol)
- Publication: final
- Timestamp: 2026-10-26T00:44+09:00
- Consumed handoff: HANDOFF-001.md
- Reviewed source base / head: `3241f267a9e4f9ed13e9664678d4f14346567f9f`, branch `chore/security-and-portability`, uncommitted snapshot identified by HANDOFF-001-SHA256.txt. All 49 entries matched before substantive review and after verification.
- Verdict: CHANGES_REQUESTED
- Scope and criteria checked: AC-01..AC-07 and the user's six review areas. Read the selected task, changed files and necessary referenced code/configuration. No application edits, commits or external writes.

## Findings

### R-01 [P2] OSV scan cannot launch Gradle on macOS/Linux

- File and line: `scripts/osv-scan.mjs:16` (also `:11`).
- Trigger / reproduction: Run the documented root command on a non-Windows machine. `mavenPackages()` passes a double-quoted executable string to `execFileSync`, while `run()` sets `shell: false` there.
- Expected and observed behavior: The wrapper should execute by its actual path. Without a shell, quotes become literal filename characters and execution fails with ENOENT before scanning.
- User or system impact: The security check is unusable on the other platforms described by SETUP.md.
- Evidence and confidence: High. An in-memory platform simulation captured the quoted Gradle executable; executing a quoted existing Node executable with `shell: false` reproduced ENOENT. No native macOS/Linux run was available.
- Suggested correction and regression check: Quote only for the Windows shell path; pass the raw executable path when no shell is used. Check both platform branches, including a checkout path containing spaces.
- Disposition: open.

### R-02 [P2] Python fallback silently omits transitive dependencies

- File and line: `scripts/osv-scan.mjs:38`–`:46`.
- Trigger / reproduction: `uv` is unavailable or `analysis-api/.venv` does not exist, including a machine prepared only with SETUP's review venv. The catch block reads requirements files and selects only literal `name==version` lines.
- Expected and observed behavior: A complete scan must use resolved installed dependencies or explicitly fail as incomplete. The fallback queries only six direct packages, omitting Starlette, Pydantic, psycopg-binary and other installed dependencies, then emits an ordinary report without a coverage warning.
- User or system impact: Vulnerabilities in omitted Python packages can be missed while the command appears successful.
- Evidence and confidence: High. Called the actual function in a Node VM with the uv invocation forced to throw; the result was exactly fastapi, uvicorn, psycopg, tzdata, pytest and httpx2. Requirements do not pin the omitted dependencies.
- Suggested correction and regression check: Require an installed/resolved environment (allow an explicit interpreter or the review venv), or stop with an actionable error. Test a missing uv/venv and assert that a successful report includes transitive packages.
- Disposition: open.

### R-03 [P2] Review runner reports success when the companion fails

- File and line: `.ai-collab/scripts/run-codex.sh:22`–`:33`.
- Trigger / reproduction: The companion exits nonzero for an authentication, configuration or runtime failure without the rate-limit text.
- Expected and observed behavior: Preserve that failure status. The script ignores the command's exit status and unconditionally exits 0 outside the retry branch.
- User or system impact: A failed review invocation is indistinguishable from a completed invocation to its caller.
- Evidence and confidence: High. Ran the script body under Git Bash with only companion discovery/root substituted in memory, `node()` returning 7, and prompt/log directed to `/dev/null`; wrapper exit was 0. No companion task or network request was made.
- Suggested correction and regression check: Capture the companion exit status and propagate non-retry failures. Evaluate retry text from the current attempt rather than the accumulated log. Check success, exit 7, rate-limit exhaustion, and a successful short attempt after a rate-limited attempt.
- Disposition: open.

### R-04 [P2] Installer-owned local settings are not ignored

- File and line: `.gitignore:53`–`:72`; `.ai-collab/SETUP.md:25`.
- Trigger / reproduction: Install GSD as documented. SETUP says its hook registration in `.claude/settings.local.json` is ignored, but there is no matching ignore rule. The existing `*.local` pattern does not match a filename ending in `.local.json`.
- Expected and observed behavior: Local installer settings should stay out of source control. `git check-ignore -v .claude/settings.local.json` returned 1, and initial `git status --short` listed it as untracked.
- User or system impact: Routine staging can include machine-local tool configuration, contrary to the portability policy. This is not evidence of an actual credential leak.
- Evidence and confidence: High; directly checked Git behavior without reading the excluded local settings file.
- Suggested correction and regression check: Ignore the intended local settings path while retaining shared `.claude/settings.json`. Recheck both paths and `git ls-files -ci --exclude-standard`.
- Disposition: open.

### R-05 [P2] Current-version documentation still describes the vulnerable versions

- File and line: `docs/10_Development_Environment 366d097bd4f78035ba4aef5e47b56378.md:72`–`:79`, `:106`–`:111`; `.claude/agents/frontend-dev.md:18`–`:20`; `.codex/agents/frontend-dev.toml:16`–`:18`; `README.md:13`.
- Trigger / reproduction: Read the current environment tables or frontend specialist instructions after this update.
- Expected and observed behavior: Current versions should match the lockfile: Vite 6.4.4, Axios 1.20.0, Router 6.30.6 and Capacitor 8.4.3. docs/10 retains all four old versions in two tables; both frontend agent definitions retain Vite 6.4.3 and Capacitor 8.4.1; README retains Axios 1.18.
- User or system impact: The documented environment and restored agent instructions disagree with the reviewed implementation, leaving the requested doc/version synchronization incomplete.
- Evidence and confidence: High. Compared current tables with package.json/package-lock.json and the security update table. Historical before/after values in docs/09 are valid and need no removal.
- Suggested correction and regression check: Update current-version declarations only. Compare these tables and both agent definitions with the resolved lockfile again.
- Disposition: open.

### R-06 [P3] README's new security command runs from the wrong directory

- File and line: `README.md:189`–`:191`.
- Trigger / reproduction: Execute the test block sequentially from the repository root. Its first line changes into spring-api; the newly added `node scripts/osv-scan.mjs` then refers to a nonexistent `spring-api/scripts/osv-scan.mjs`.
- Expected and observed behavior: The scan must run from the repository root, as it uses `process.cwd()` to locate all three projects. Merely changing the script path to `../scripts/...` would still leave that assumption broken. The existing following `cd analysis-api` also needs a root-relative context.
- User or system impact: Copying the documented verification block fails.
- Evidence and confidence: High; checked the command sequence, script root handling and file locations.
- Suggested correction and regression check: Use subshells for each project command and keep the scan at root. Check every command's working directory without needing network execution.
- Disposition: open.

## Verification

All reviewer commands below used the same dirty snapshot on Windows. Application tests/builds exited 0. Network-dependent checks were deliberately not rerun.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | SHA-256 of every manifest entry, repository root | 49/49 match, checked twice | — | No committed head for this task |
| AC-01 / Spring versions | `gradlew.bat dependencies --configuration runtimeClasspath --offline`, spring-api; `GRADLE_USER_HOME=E:\workspace\daipetto\spring-api\.gradle\review-home` | Exit 0. Boot 3.5.16, Framework 6.2.19, Security 6.5.11, Data Commons 3.5.13, Hibernate 6.6.53.Final, Flyway 11.7.2. Overrides effective: Tomcat 10.1.61, Jackson BOM/modules 2.21.8 (annotations 2.21), PostgreSQL JDBC 42.7.14, Log4j API/bridge 2.25.5 | — | `spring-boot-devtools:3.5.16 FAILED` in the report: unavailable in offline cache. Exit 0 does not mean every artifact resolved; production dependency overrides were visible |
| AC-01 / npm and images | Scripted package/lock comparison; inspect all three Dockerfiles and compose | Root dependency maps match; lock versions match the announced upgrades. Tags: node:24.22.1-alpine, python:3.12.15-slim, postgres:16.15; Spring remains eclipse-temurin:17-jdk | OSV after-report lists three affected packages | No live OSV/npm audit or image availability verification |
| AC-01 / accepted findings | Search Spring main sources/config for XSLT, SSE and view rendering; inspect frontend entrypoint, routing/navigation producers, Tailwind inputs | No XSLT/SSE use found. Client `createRoot` + BrowserRouter; no SSR/hydration entrypoint. Link/navigation destinations are fixed paths or paths with fixed prefixes; mapped navigation items come from local constants. Tailwind scans checked-in index/src files. The three reachability rationales fit current code | Advisory IDs, affected ranges and fix versions in `evidence/osv-after.json` / docs/09 | Reachability reviewed; live advisory metadata not revalidated offline |
| AC-02 | `gradlew.bat test --offline --rerun-tasks`, spring-api, same GRADLE_USER_HOME | Exit 0; fresh JUnit XML totals: **382 tests, 0 failures/errors/skips**; JaCoCo generated. Two MapStruct mapping warnings, no MockBean removal warnings | Handoff also reports 382 | H2/mocks do not prove PostgreSQL behavior |
| AC-02 / conversion | Normalize old imports/annotations and compare all 16 controller test files | Every file is an exact MockBean import/annotation substitution; unchanged mocking setup/assertions, with contexts exercised by the passing suite | — | No additional semantic changes found |
| AC-03 | Read API evidence summary | Not rerun | **134/134**, scratch PostgreSQL, `evidence/api-regression-summary.json` | Reported-only database regression |
| AC-04 | `.review-venv/Scripts/python.exe -m pytest -q`, analysis-api | Exit 0; **38 passed**. One pytest cache permission warning | E2E **17/17**, `evidence/analysis-e2e-result.json` | E2E/PostgreSQL reported-only |
| AC-05 | `npm.cmd run build`, frontend | Exit 0; TypeScript + Vite 6.4.4 build successful | S-01..S-16 browser regression in HANDOFF-001 | Browser/native runtime not rerun |
| AC-06 | Inspect Docker/compose changes | Tags consistent with update | Analysis/frontend Docker builds successful per HANDOFF-001 | Docker builds reported-only |
| AC-07 / tools | Inspect SETUP, settings, skills-lock, runner and agent policy changes | Plugin declarations/skill sources present; no further concrete declaration defect established. Runner failure reproduced as R-03 | Installer availability/help checks in handoff | No online installer/plugin restoration test |
| AC-07 / ignores and secrets | `git ls-files -ci --exclude-standard`; inspect manifest files and scan common private-key/token signatures | Exit 0, no tracked paths listed. No credential/private-key material found in scoped changes; signature scan had no hits. Local ignore omission is R-04 | — | Global Git ignore file was unreadable (warning); excluded user files were not inspected |
| Scripts | Actual OSV collection functions in isolated Node VM; notion_prep imported and output redirected to memory | R-01/R-02 reproduced without network. notion_prep converted all 13 docs in memory; title removal, tag escaping, inline-code preservation, page mentions and bold checks passed; zero bold warnings | — | No remote Notion write/render verification |
| Doc synchronization | Changed docs/09/10/11/12, README, TODO, AGENTS, skill and specialist diffs | Spring/DB/Python versions and MockitoBean rules synchronized; frontend omissions R-05 and README command R-06 remain | — | Historical statements treated as historical |

## Remaining risks and next action

Claude Code should address R-01..R-05 and correct R-06 in a new handoff with new hashes. Keep this round intact. R-06 is minor; the P2 findings drive CHANGES_REQUESTED. The passing application suites support the dependency update and annotation conversion, but do not resolve the tooling and documentation defects.

OSV lookup, PostgreSQL regression, E2E, browser regression and Docker builds remain implementer-reported. The three accepted dependency findings are still present; current-code reachability supports their rationale, not a claim that they were fixed. Offline devtools cache coverage and a real new-machine restoration remain unverified. Spring Boot 4 / Router 7 / Tailwind 4 follow-ups and the intentional symptoms/summary gap remain outside this review's implementation scope.

Acceptance has not been granted; no merge, deployment or application correction was performed. Review-owned output is this file only; requested build/test output was generated, and the prepared review cache/interpreters were preserved.
