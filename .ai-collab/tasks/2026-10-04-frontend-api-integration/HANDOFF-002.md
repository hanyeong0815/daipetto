# Handoff 002: 2026-10-04-frontend-api-integration

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-04, Asia/Tokyo
- Task specification: none (same as HANDOFF-001)
- Recipient: Codex
- Requested action: re-review of F-01, F-02 and F-03
- Consumed review: [REVIEW-001.md](REVIEW-001.md) (not edited)
- Source base SHA: 9c2ae519f4a619e1fd232bb68b624df68efddf33 (`origin/develop`)
- Source head SHA: same; all changes are uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Checkout/branch: `feat/react-api-integration`, dirty, not pushed
- Snapshot: `HANDOFF-002.patch` (`git diff HEAD -- AGENTS.md TODO.md docs frontend`, new files intent-added; 27 files). `HANDOFF-002-SHA256.txt` has 30 entries: those 27 plus `scripts/seed-fe.mjs`, `scripts/README.md` and `scripts/fix-acceptance.cjs`. HANDOFF-001 is untouched.
- Documents: I kept Codex's user-authorized TODO.md/AGENTS.md edits and added only my status lines (F-01..F-03 fix notes, browser regression item, one sentence on the AGENTS.md frontend review line).

## Change summary

Files changed since HANDOFF-001: `stores/authStore.ts`, `stores/petStore.ts`, `stores/hospitalStore.ts`, `pages/LoginPage.tsx`, `pages/ReservationPage.tsx`, `pages/DashboardPage.tsx`, `pages/MyPetPage.tsx`, `utils/date.ts`, docs/05, TODO.md, AGENTS.md, and the task scripts.

| Finding | Change |
|---|---|
| F-01 | `authStore` gains `session` (not persisted), incremented by every `clearAuth()`: logout, refresh failure in the interceptor, and now also successful login. `LoginPage` calls `clearAuth()` before `setTokens()`, so logging in as B without logging out starts a new session. Token refresh only calls `setTokens()` and keeps the session. `petStore` resets `pets`/`selectedPet`/`isLoading`/`error` when the session changes, via an `authStore` subscription. Each fetch (list, detail, remove) captures the session at start and commits only if it is unchanged, so a late A response cannot repopulate B. MyPetPage now shows a fetch failure as an error with 再試行 instead of the "no pets yet" empty state. |
| F-02 | `ReservationPage` is a wrapper rendering `ReservationForm key={hospitalId}`, so a route change remounts it and every selection and step resets. Hospital detail and schedules are loaded into that component's local state through an effect with an `active` flag; responses arriving after a switch or unmount are discarded. "次へ" stays disabled until the current hospital loads. Load failure shows the message and 再試行, and a failed submit reloads. The shared `hospitalStore` (still used by HospitalDetailPage, AdminHospitalPage and the search page) now commits only the latest request per resource (list, detail, schedules, business hours). When a different hospital is requested it clears the previous hospital's detail/schedules/hours at once; a refetch of the same hospital keeps its data visible. |
| F-03 | `utils/date.ts` gains `isUpcoming(date, startTime, now?)`: the slot start (`YYYY-MM-DDTHH:mm`) is strictly after the current JST time, compared at minute precision, the same rule as `reservationDatetime.isAfter(now)` in CreateReservationService. `todayInJapan(now?)` accepts a clock for tests. ReservationPage uses `isBookable = AVAILABLE && isUpcoming` to build the date chips, to disable time buttons (legend now 受付停止中・受付終了), to gate "次へ", and to re-check before submit, which shows a message and clears the slot. The dashboard's "next reservation" is the earliest REQUESTED/APPROVED with `isUpcoming`. The dashboard also now distinguishes fetch failures from empty data. |

Docs: docs/05 SC-010 states the slot rule, the load gating and the retry. No API, DB or other spec change.

## Acceptance evidence

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Type check + bundle | `npm run build`, `frontend/` (final code) | Node 24.17.0 | exit 0 |
| Store/date regression | `node .ai-collab/tasks/2026-10-04-frontend-api-integration/scripts/fix-acceptance.cjs`, repository root | real TS stores + real Zustand, mocked HTTP | 10/10 PASS |
| Harness detects the defects | same script, with `petStore.ts` and then `hospitalStore.ts` temporarily replaced by their HEAD versions (restored and hash-checked afterwards) | same | fails on the first F-01 check (`[{"id":11,"name":"Private pet A"}]` ≠ `[]`), then on the first F-02 check (`[101]` ≠ `[202]`) |
| Reviewer reproduction | `review-store-repro.cjs`, unmodified | same | exit 1 at module loading: `petStore` now imports `./authStore`, which that harness does not map |
| Browser regression | Vite :5173 → boot jar (HEAD) :18081, fresh `daipetto_fe_1004`, `scripts/seed-fe.mjs`; XHR delay/failure injected in the page for selected URLs | In-app Chromium, 2026-10-04 ~21:10–21:20 JST | rows below |
| Capacitor | `npx cap sync`, then `./gradlew assembleDebug` with the Android Studio JBR 21 | Capacitor 8.4.1, AGP 8.13.0 | exit 0; APK 4,248,960 bytes, SHA-256 `8151a4f0e830fadb5599203de92ce74ac56116add66927cc5ee19ba5d259e3be`, contains `index-4oP0ufKN.js` = final `dist` |

The fixed-clock F-03 cases run against 2026-10-04 JST:

| JST clock | Expected and observed |
|---|---|
| 15:00 | 09:00, 14:30 and 15:00 that day are not bookable; 15:30 that day and 09:00 the next day are; the previous day's 23:30 is not; `HH:mm` input works |
| 23:59:30 | `todayInJapan` is 10-04; 23:30 that day is not bookable; 00:00 on 10-05 is |
| 00:00:00 on 10-05 | `todayInJapan` is 10-05; 00:00 that day is not bookable (strict); 00:30 is |
| 08:30 on 10-05, UTC date still 10-04 | 08:00 is not bookable; 09:00 is |

Browser observations:

```text
F-03 dashboard   seeded APPROVED reservation that started 00:30 today is skipped; "次の予約" = 10月6日（火） 09:00
F-03 slots       hospital 1, date 2026-10-04: 00:00 disabled, 00:30 disabled, 23:30 enabled (clock ~21:10 JST)
F-02 reorder     /hospitals -> /hospitals/1/reserve (hospital 1 GETs delayed 3 s) -> /hospitals/2/reserve after 200 ms,
                 wait 4 s so hospital 1 responses land last: dates [2026-10-08], slots [14:00],
                 confirm "みどり動物クリニック / 2026-10-08 14:00〜14:30"
F-02 failure     hospital 1 loaded, then /hospitals/2/reserve with its schedules GET failing:
                 "病院情報・予約枠の取得に失敗しました。" + 再試行; 次へ disabled even with a pet selected;
                 no hospital 1 dates; after 再試行 (failure removed): 次へ enabled, dates [2026-10-08]
F-01 switch      user A /pets shows モモ; pets GET set to fail; logout; login as fe-user2 through the form:
                 dashboard greets はなこ, no モモ anywhere; /pets shows no モモ; after recovery only クロ
F-01 new session re-login as fe-user2 with pets GET failing: "ペット一覧の取得に失敗しました。" + 再試行 -> クロ
Normal flows     fe-user2 books hospital 2 10-08 14:00 -> history row 申請中; health record for クロ with default date
                 2026-10-04 (JST), 9.4 kg, symptom -> listed; logout works (used for the switches above)
Console          only errors: the injected failures (net::ERR_UNSAFE_PORT to 127.0.0.1:1)
```

The slow-A-response-after-B-login case is covered by the harness, not by the browser run. `window.confirm` was stubbed during logout. React StrictMode in dev doubles effect requests; the `active` guard discards the first set. After the run, both servers were stopped, the scratch DB was dropped and the temporary launch entries were reverted. Port 8080 and `daipetto` were not used.

## Limitations

- The re-check before submit was not triggered in the browser, because it needs a slot to start while the confirmation step is open. Its predicate is covered by the fixed-clock cases.
- Frontend still has no test framework. `fix-acceptance.cjs` is a manual Node harness like the reviewer's and is not wired into a build.
- Only `petStore` holds per-user data globally today. Other per-user data lives in page-local state, which is discarded when the page unmounts on logout. A future private store must subscribe to `session` the same way; the comment in `authStore` says so.
- The admin reservation list API decision, pet editing, Android device/emulator runtime and iOS build are unchanged from HANDOFF-001 and TODO.md.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| F-01 | Fixed | harness F-01 ×4 (incl. late A list/detail after B login with failed B fetch); browser account switch with failing B fetch | re-review |
| F-02 | Fixed (page keyed/local + store latest-wins) | harness F-02 ×4; browser reordered responses and failed-B-after-A with retry | re-review |
| F-03 | Fixed (`isUpcoming`, dashboard included) | harness fixed clocks incl. rollover; browser same-day past/future slots and dashboard | re-review |
| Dashboard error vs empty (suggestion) | Done | dashboard shows 取得できませんでした on failure | none |

## Receiver instructions

Verify against `HANDOFF-002-SHA256.txt` from the repository root, or apply `HANDOFF-002.patch` onto 9c2ae51. Run `fix-acceptance.cjs` from the repository root; browser setup is in `scripts/README.md`. Source write ownership returns to no one with this round. No commit, push or merge is implied.
