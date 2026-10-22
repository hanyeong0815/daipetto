# Review 002: 2026-10-22-hospital-admins

- Author / reviewer: Codex
- Publication: final
- Timestamp: 2026-10-22 (Asia/Seoul)
- Consumed handoff: [HANDOFF-002.md](HANDOFF-002.md), response to REVIEW-001.md
- Reviewed source base / head: `71c0fe8cb05759338bc6ecd669713394fa6941b0`, branch `feat/spring/hospital-admins`, uncommitted `git diff HEAD` snapshot identified by `HANDOFF-002-SHA256.txt`.
- Verdict: ACCEPTED
- Scope and criteria checked: R-01, R-02, R-03 and regressions in the round-2 changes; AC-07 selection, mutation completion, failed-load recovery and documentation. Round-1 backend conclusions retain their recorded limitations.

## Findings

No new actionable regression found in the reviewed changes.

### R-01 [P2] A completed suspension can replace the selected hospital with the previous hospital's data

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:59`, `:137`, `:156`, `:187`, `:212`, `:227`, `:244`, `:346`.
- Disposition: resolved.
- `selectHospital` synchronously updates the ref and state. Its callers cover dropdown selection (including null), single-hospital auto-selection and selection after creation. There is no direct setter bypass. Auto-selection checks the current ref after the request, preserving a selection made while the list was pending. Creation intentionally selects the new hospital through the same helper.
- Every existing-hospital mutation captures its target before awaiting: basic-info save, suspension, business-hours create/update/delete, and schedule create/block/unblock. Each per-hospital refresh uses `refreshIfSelected`. Save/suspend also refresh the managed list without selecting another hospital. Requests already started before a switch remain protected by the store's per-resource request counters.
- The ID equality render guard encloses the basic-info form, keyed admin section, business hours and schedules. Null selection hides these controls. Assignment/unassignment refreshes only its keyed child instance and cannot change parent selection or shared hospital data.
- Reviewer reproduction: actual store transpiled in memory with installed TypeScript and real Zustand, stub APIs with manually resolved promises, and the selection/refresh helper extracted from the current page. After selecting/loading B, A's completion issues zero new detail/schedule/hours requests. A requests already in flight and resolved after B cannot replace B's data. Null selection fails the render predicate; same-hospital refresh still runs. Assertions passed, exit 0. This is a Node logic/store check, not React DOM or browser verification.

### R-02 [P2] Failed managed-hospital loading is displayed as no affiliation and offers no retry

- File and line: `frontend/src/pages/admin/AdminHospitalPage.tsx:78`, `:312`, `:318`.
- Disposition: resolved.
- Initial failure preserves `hospitals === null` and sets the failure flag. Retry clears the error and invokes the same fetch function. Only a successful empty array triggers the no-affiliation/create-hospital message. Failed refresh preserves an already loaded list.
- Reviewer reproduction: executed the current page's extracted `fetchHospitals` callback with state-setter/API stubs. Rejected initial request retained null plus failure; retry with one hospital cleared failure and auto-selected it; later rejected refresh retained that list; successful empty response produced an empty list with failure false. Assertions passed, exit 0. Retry JSX and empty-state conditions were inspected separately; browser evidence remains reported-only.

### R-03 [P3] Shared test policy still says no PostgreSQL concurrency test exists

- File and line: `AGENTS.md:154`.
- Disposition: resolved.
- Section 8 now distinguishes the H2/mock Gradle suite from manual PostgreSQL race scripts and qualifies their results as implementer-reported unless rerun by a reviewer. Hospital-admin HA-12/HA-13 in the selected script and docs/11 HADM-T005/T008 agree. The referenced user-management task was not reopened in this focused review.

## Verification

Current checks apply to the manifest-verified snapshot above. Environment: Windows PowerShell, Node 24.17.0, installed TypeScript/Zustand, Vite 6.4.3.

| Criterion | Command/check and working directory | Reviewer-executed outcome | Reported-only evidence | Gap |
|---|---|---|---|---|
| Snapshot | Root: parse manifest and `Get-FileHash -Algorithm SHA256`; branch, HEAD, status | 86/86 matched before source review; exit 0. Correct branch and HEAD | None | Dirty snapshot; unrelated `.vscode/` and `spring-api/.claude/` excluded |
| Round-2 scope | Root: Node comparison of round-1/round-2 manifests and patch against `git diff HEAD -- AGENTS.md TODO.md docs spring-api frontend` | Only AGENTS.md, docs/05, AdminHospitalPage and captured patch differ. Patch equals current diff after CRLF normalization; exit 0 | None | No backend source/test/migration changes since round 1 |
| Frontend build | `frontend/`: `npm.cmd run build` | Exit 0; TypeScript/Vite passed, 136 modules | Handoff also reports passing build | Does not prove browser behavior |
| R-01 / R-02 | `frontend/`: PowerShell here-string piped to Node, in-memory source extraction/transpilation and assertions described above | Exit 0; both checks printed PASS. No test files added | Patched-XHR browser save/suspend and failed-load/retry runs | No reviewer browser run or React effect scheduling assertion |
| R-03 / docs | Read AGENTS section 8, docs/05 SC-014, docs/11 evidence note and selected scripts | Wording agrees with implementation and manual-check scope | Manual PostgreSQL results | PostgreSQL not rerun |
| Spring | Not rerun: unchanged backend hashes | Prior reviewer run in REVIEW-001: 301 tests, zero failures/errors/skips; not a fresh run | HANDOFF-002 reports backend acceptance 13/13 | H2/mocks do not prove PostgreSQL races; review-home retained |
| PostgreSQL / browser | Not run this round | No new reviewer DB/browser evidence | HANDOFF-002 acceptance and browser results only | Docker not reprobed; prior engine-access failure recorded in REVIEW-001 |

## Remaining risks and next action

- R-01 through R-03 are closed for this snapshot. Claude Code, as coordinator, should consume this record and update task status at the next handoff boundary. No commit, push or merge performed.
- PostgreSQL and browser evidence remains reported-only. Unpaged lists, first-100 admin candidate cap and previously documented authorization check-then-act window remain unchanged limitations.
- Only REVIEW-002.md was authored; build output was regenerated by verification. Application code, product docs and other task records were not edited. `spring-api/.gradle/review-home` was not deleted.
- ACCEPTED covers the selected scope and evidence, not a guarantee that all defects are impossible or that integration occurred.
