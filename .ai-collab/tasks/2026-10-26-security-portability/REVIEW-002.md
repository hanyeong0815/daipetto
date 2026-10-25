# Review 002: 2026-10-26-security-portability

- Author / reviewer: Codex (gpt-6.1-sol)
- Publication: final
- Timestamp: 2026-10-26T00:51+09:00
- Consumed handoff: HANDOFF-002.md
- Reviewed source base / head SHA or dirty snapshot hashes: `3241f267a9e4f9ed13e9664678d4f14346567f9f`, branch `chore/security-and-portability`; uncommitted snapshot identified by HANDOFF-002-SHA256.txt. All 52 entries matched before substantive review and again before completion.
- Verdict: ACCEPTED
- Scope and criteria checked: Round-2 fixes for R-01..R-06, their regression risk, and the requested offline checks. Startup policy and the selected task/referenced files only. No application changes or integration actions.

## Findings

No new actionable findings. R-07 is not assigned.

### R-01 [P2] OSV scan cannot launch Gradle on macOS/Linux

- Disposition: resolved.
- Evidence: `scripts/osv-scan.mjs` now passes the raw executable and arguments with `shell: false` outside Windows, and quotes them for the Windows shell. The root derives from `import.meta.url`, not the caller's working directory. Isolated execution of the actual collection code with Windows/Linux platform branches confirmed wrapper paths, arguments, working directories and Python candidates for checkout paths containing spaces. Actual Node execution from `C:/Program Files/nodejs/` succeeded with both the Windows shell branch and the raw executable branch.
- Limit: Linux/macOS and a real Gradle invocation from a checkout containing spaces were not run. The platform simulations are not native OS validation.

### R-02 [P2] Python fallback silently omits transitive dependencies

- Disposition: resolved.
- Evidence: The requirements-file fallback is removed. `pypiPackages()` reads installed distributions through `importlib.metadata`; explicit interpreter selection and local/review venv candidates are present. Calling the actual function with `--python analysis-api/.review-venv/Scripts/python.exe` returned 26 distributions, including FastAPI, Starlette, Pydantic and psycopg-binary. The requested real missing-interpreter command exited 2 with actionable guidance. A separate guarded execution of the script's startup path confirmed exit 2 with zero child-process calls and zero fetches.
- Limit: Automatic selection finds the existing `.venv`, whose external uv-managed base interpreter cannot launch under this sandbox (child exit 101; Node fails without producing a report). Explicit `.review-venv` selection works. This environment access restriction does not recreate the silent incomplete scan; the protected environments were preserved.

### R-03 [P2] Review runner reports success when the companion fails

- Disposition: resolved.
- Evidence: `run-codex.sh` captures the companion's status, checks only the current attempt's output for rate-limit text and propagates a non-retry failure. The new self-check ran successfully: success 0, non-retry failure 7, limit then success 0, exhausted limit 2, success with an old limit in the log 0, missing companion 1. Overall exit 0. Only fake companions ran; no real review invocation or network was used.

### R-04 [P2] Installer-owned local settings are not ignored

- Disposition: resolved.
- Evidence: `git check-ignore -v .claude/settings.local.json` returned exit 0 and `.gitignore:56:.claude/settings.local.json`. Shared `.claude/settings.json` returned exit 1 (not ignored). `git ls-files -ci --exclude-standard` returned exit 0 with no paths.
- Limit: Git warned that the user's global ignore file was inaccessible. The repository-local rule was directly confirmed; local settings contents were not read.

### R-05 [P2] Current-version documentation still describes the vulnerable versions

- Disposition: resolved.
- Evidence: Both frontend tables in docs/10 declare Vite 6.4.4, Axios 1.20.0, Router 6.30.6 and Capacitor 8.4.3. Both frontend-dev definitions match these versions; README now says Axios 1.20. Programmatic comparison confirmed all four resolved versions in `frontend/package-lock.json` and their presence in docs/10 and both agent definitions. The changed declarations were also inspected directly.

### R-06 [P3] README's new security command runs from the wrong directory

- Disposition: resolved.
- Evidence: README's test block puts each project command in a subshell and keeps the scan at root. Executed the extracted block in Git Bash with only the expensive test/scan commands replaced in memory by directory/file assertions. Spring, Python and final repository-root checks all passed. No application test or network scan was invoked by this check.

## Verification

All commands ran from `E:/workspace/daipetto` against the snapshot above unless specified. Environment: Windows, Node v24.17.0, Git Bash 5.3.9. The reviewer Node version differs from the documented target 24.22.1.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | PowerShell `Get-FileHash -Algorithm SHA256` for every HANDOFF-002 manifest path, root | 52/52 match before review and at completion | None needed | Dirty snapshot, not a task commit |
| Change boundary | Compare HANDOFF-001 and HANDOFF-002 manifest hashes | All 21 application/dependency paths in the latter are unchanged | No application changes claimed by implementer | Comparison covers published manifest paths |
| R-01 | Actual OSV functions in isolated Node VM; Windows/Linux branches and space-containing paths; actual Node child execution | Assertions passed; both command execution modes returned 42 | Full Windows scan from frontend: 406 packages, 3 affected | No native Unix Gradle run or live OSV lookup |
| R-02 | `node scripts/osv-scan.mjs --python /nonexistent/python`, root | Exit 2; actionable missing-environment message | Same result reported | Guarded startup additionally proved no subprocess/fetch before exit |
| R-02 | Actual `pypiPackages()` with explicit review interpreter, in-memory harness | Exit 0; 26 distributions; transitive package assertions passed | Default implementer venv: 26 | Default venv base interpreter inaccessible here; no scan was generated from that failed invocation |
| R-03 | `C:/Program Files/Git/bin/bash.exe .ai-collab/scripts/test-run-codex.sh`, root | Exit 0; all six cases passed | Same six cases reported | No live companion task |
| R-04 | `git check-ignore -v` for local/shared settings; `git ls-files -ci --exclude-standard`, root | Local ignored (0), shared not ignored (1), no tracked ignored paths (0) | Same reported | Global ignore file permission warning |
| R-05 | Lockfile assertions, doc tables and both agent definitions; README diff | Versions consistent | Same corrections reported | No package installation needed |
| R-06 | Extract README test block and substitute command bodies with cwd assertions in memory; execute with Git Bash | Exit 0; all three working-directory checks passed | File review reported | Actual JUnit/pytest/OSV commands not repeated |

## Remaining risks and next action

No required round-2 fix remains. The coordinator can record acceptance of this frozen snapshot and proceed with the user's chosen integration workflow. This review does not commit, merge or deploy it.

Application suites were not rerun in round 2 because the published application/dependency hashes are unchanged. REVIEW-001 remains the source for reviewer-executed JUnit 382, pytest 38 and frontend build evidence. PostgreSQL API regression, analysis E2E, browser regression, Docker builds and the full network OSV scan remain implementer-reported. The three known dependency findings retain the reachability rationale reviewed in round 1; no claim is made that they were fixed. Native Unix execution and a real new-machine restoration remain unverified.

Only this review record was written by the reviewer. `spring-api/.gradle/review-home`, `analysis-api/.review-python` and `analysis-api/.review-venv` were preserved. Intentional practice gaps remain outside scope.
