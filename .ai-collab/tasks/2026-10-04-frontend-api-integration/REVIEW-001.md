# Review 001: Frontend API integration

- Reviewer: Codex
- Publication: final
- Date: 2026-10-04
- Consumed handoff: HANDOFF-001.md
- Source: `feat/react-api-integration`, HEAD `9c2ae519f4a619e1fd232bb68b624df68efddf33` plus HANDOFF-001.patch. All 25 manifest entries matched before review.
- Verdict: **CHANGES_REQUESTED**
- Scope: frontend API wrappers/types, reservation/history, health records, notifications, dashboard, logout, shared stores and date handling; backend contracts used for comparison.
- Ownership: application review only. The user separately authorized Codex to update TODO.md; Codex also synchronizes the corresponding AGENTS.md status. These documentation changes intentionally supersede their handoff hashes. Implementer source and handoff remain untouched.

## F-01 [P1] Account switching retains another user's pet cache

- Location: `frontend/src/components/layout/TopAppBar.tsx:20-26`; supporting paths `frontend/src/stores/authStore.ts:29-30`, `frontend/src/stores/petStore.ts:22-33`.
- Logout only clears auth state. The global pet list and selectedPet survive. Log in as B without reloading after A used the app: Dashboard reads A's cached pets immediately; if B's list request fails, MyPetPage finishes loading and still renders A's names/weights. An in-flight A request can also complete after B login and repopulate the cache, even if a simple reset is added.
- Reviewer reproduced both retention and late-response overwrite using the actual TypeScript Zustand stores with controlled HTTP promises. This is client-side disclosure on a shared browser, not a bypass of the backend owner checks.
- Fix: reset private stores on session change and reject responses belonging to an earlier session (generation/user key or cancellation with a guarded commit). Include selectedPet and any future private cache. Regression: A -> logout -> B, slow A response and failed/slow B fetch; no A data may render.
- The underlying cache behavior predates this branch; the new logout and dashboard flows expose it in the reviewed integration.

## F-02 [P2] Reservation screen can use schedules from a different hospital

- Location: `frontend/src/pages/ReservationPage.tsx:28-42`; supporting source `frontend/src/stores/hospitalStore.ts:54-61`.
- The page reads one global schedules array, starts a new fetch without clearing/keying it, and does not consume its error state or restrict schedule ownership. A late request for hospital A overwrites B's array, or B's request fails and A's previous slots remain selectable. The page submits B's route hospitalId with A's scheduleId; the backend correctly rejects the mismatch, so no wrong-hospital reservation is created, but the UI displays invalid choices and cannot complete them.
- Actual-store harness reproduced B's response arriving first, followed by A overwriting it. selectedHospital has the same unkeyed response pattern, so confirmation text can also become stale.
- Fix: key detail/schedule state by hospitalId, ignore obsolete responses, clear selections when the route changes, and block progression until current-hospital loading succeeds. Display fetch errors and retry. Test reordered responses plus failed B fetch after a successful A visit.

## F-03 [P2] Same-day past times remain bookable in the UI

- Location: `frontend/src/pages/ReservationPage.tsx:35-40` and time-button `isBlocked` condition; related `frontend/src/pages/DashboardPage.tsx:37`.
- Dates are filtered only by `availableDate >= today`; slot buttons disable only non-AVAILABLE status. At 15:00 JST a 09:00 AVAILABLE slot today remains enabled and can reach confirmation, although CreateReservationService explicitly requires its start timestamp to be after now and returns RESERVATION-005. Retrying does not remove it because it remains AVAILABLE.
- The dashboard likewise chooses an earlier same-day REQUESTED/APPROVED reservation ahead of a genuinely upcoming one.
- Evidence: inspected frontend predicates against backend `reservationDatetime.isAfter(LocalDateTime.now())`; this case was not browser-reproduced by the reviewer.
- Fix: compare the combined date/start time to current JST, both for available-date construction and slot selection, and revalidate before submit. Apply an explicit upcoming-time rule to the dashboard. Test a fixed afternoon clock with past/future slots on the same day and a day rollover.

## Other assessment

- New wrapper endpoints, methods, identifiers, status values and response fields match the inspected Spring DTO/controller contracts. HealthRecord blank text versus omitted weight handling follows the documented PATCH policy; input bounds match weight/symptom validation.
- Reservation cancellation is shown only for REQUESTED/APPROVED; failures reload state. Notifications use recipient-scoped APIs and render strings normally (no raw HTML path found). Admin route guards remain in place.
- Already-booked AVAILABLE slots are a disclosed backend availability-contract limitation, separate from F-03: there is no reserved flag to filter locally.
- AdminReservationPage/AdminUserPage remain mocks, Pet editing has no screen, and shared reservation/notification stores are not required for the current local-state implementation. Preserve these distinctions in TODO rather than calling all screens connected.
- New data mutations generally show API error messages. Reservation prerequisite loading/error handling is insufficient as described in F-02. Dashboard failures currently collapse to empty data; consider separating errors from an actual empty result.

## Verification

| Check | Reviewer-executed outcome |
|---|---|
| HANDOFF-001-SHA256.txt | 25/25 matched before documentation updates |
| `npm run build`, `frontend/` | Exit 0; TypeScript + Vite pass, 132 modules |
| `node .ai-collab/tasks/2026-10-04-frontend-api-integration/scripts/review-store-repro.cjs`, repository root | Exit 0; F-01 and F-02 reproduced using real store code, real Zustand and mocked transport; assertions describe defects, not acceptance |
| API contract inspection | New reservation, health-record and notification wrapper/type paths checked against backend contracts |

No application test framework was installed. The harness is a narrow Node reproduction, not browser E2E. The implementer's desktop/mobile browser walkthrough, Capacitor sync and Android APK build are **reported-only** here. Reviewer did not re-run browser/native-device flows or unchanged Spring tests. A successful build does not cover slow requests, account switching or same-day time eligibility.

## Documentation changes and next action

TODO.md now separates implemented wiring from review acceptance, tracks F-01..F-03 and browser/native verification, retains admin API/design blockers and records the previously accepted backend review. AGENTS.md gets a matching frontend review status. No numbered product specification needs a new contract entry because this round changes no API or product behavior.

Claude should fix F-01..F-03 and publish a fresh snapshot/handoff with adverse-request-order and clock-boundary verification. Preserve this review. No commit, push, merge or deployment is implied.
