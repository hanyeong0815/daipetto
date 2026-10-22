# Review 002: frontend integration fixes

- Reviewer: Codex
- Publication: final
- Date: 2026-10-04
- Consumed handoff: HANDOFF-002.md
- Source: `feat/react-api-integration`, HEAD `9c2ae519f4a619e1fd232bb68b624df68efddf33` plus HANDOFF-002.patch.
- Identity: all 30 HANDOFF-002-SHA256.txt entries matched before review and at completion.
- Verdict: **ACCEPTED** for the F-01/F-02/F-03 fixes and inspected integration changes in this snapshot.
- No new actionable finding. Application code, TODO.md, AGENTS.md and implementer-owned records were not modified this round.

## Dispositions

| ID | Status | Evidence |
|---|---|---|
| F-01 previous account's pet cache survives | Resolved | clearAuth advances a non-persisted session generation. Successful login explicitly advances it before installing new tokens. petStore subscribes to changes and clears list/detail/loading/error; list/detail/remove responses check their originating session. Executed tests cover immediate clearing, failed B fetch, late A list/detail and login without prior logout. Token refresh preserves the generation as intended. |
| F-02 previous hospital's schedules overwrite current hospital | Resolved | ReservationForm is keyed by hospitalId and owns its detail/schedules locally. Effect cleanup rejects obsolete responses; initial progression requires successful loading; error/retry is visible. Shared hospitalStore also clears data on hospital change and commits only the latest request per resource. Executed tests cover response reversal, B failure after A, detail clearing and same-hospital refetch. |
| F-03 past same-day slots offered | Resolved for the current minute-based slot flow | isUpcoming compares JST date plus time. The same predicate filters offered dates, disables past/blocked slots, gates progression and runs again before submission. Dashboard uses it too. Executed fixed-clock tests include afternoon, current minute, midnight rollover and the JST/UTC date difference. |

The session subscription and response guards address both parts of F-01; clearing arrays alone would not have addressed late responses. The hospital page's local keyed state also protects its confirmation and selections, rather than only changing the shared-store array.

The reservation submit handler explicitly rejects an expired selection and clears its id before any create request. This handler was inspected; reviewer did not simulate an actual clock rollover while a browser confirmation screen was open.

## Reviewer verification

| Command/check | Working directory | Actual result |
|---|---|---|
| HANDOFF-002 manifest verification | Repository root | 30/30 matched |
| `node .ai-collab/tasks/2026-10-04-frontend-api-integration/scripts/fix-acceptance.cjs` | Repository root | Exit 0, 10 checks passed |
| `npm run build` | frontend/ | Exit 0, TypeScript + Vite; 132 modules; `index-4oP0ufKN.js` |
| Code inspection | Auth/login, pet/hospital stores, ReservationPage, DashboardPage, date helper | Fixes and their call sites agree with the stated behavior |

The acceptance harness was inspected: it transpiles the actual source and runs real Zustand stores with controlled HTTP promises, rather than reimplementing their behavior. The date tests execute the actual utility with fixed clocks. These tests establish store and predicate behavior; they are not React/browser integration tests. The old reviewer defect harness was not rerun: its import mapping does not include the newly introduced authStore dependency, so its module-loading failure would not establish a fix.

## Evidence limits and follow-up

- Browser switching/retry/normal-flow observations and the Android APK build in HANDOFF-002 remain **implementer-reported** in this review. Codex did not independently run a browser, Capacitor sync, emulator/device or iOS build.
- The manual harness is not part of a frontend CI/test command. Keep its adverse-order and clock-boundary scenarios when introducing durable test automation.
- The date helper assumes slot starts are minute-aligned, matching the current UI; if second-resolution scheduling is supported as a product flow, compare at that precision and extend boundary tests.
- Admin reservation listing, admin user APIs, pet-edit screen/contracts, native runtime checks and existing design decisions remain outside these fixes, as recorded in TODO.md. Acceptance does not mark those items complete.
- No unchanged backend suite was rerun. No commit, push, merge or deployment was performed.

Claude can consume this result and update the coordinator's status at the next handoff/integration boundary. Preserve this review and its source identity; acceptance does not extend to later source changes.
