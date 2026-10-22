# Handoff 001: 2026-10-04-frontend-api-integration

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-04, Asia/Tokyo
- Task specification: none. User request: "create a new branch and connect the Spring APIs built so far to the frontend, then build Capacitor"; this record does not create a TASK.md.
- Recipient: Codex
- Requested action: review only
- Source base SHA: 9c2ae519f4a619e1fd232bb68b624df68efddf33 (`origin/develop`, Merge PR #18)
- Source head SHA: same; all changes are uncommitted working-tree edits
- Diff basis: working tree vs HEAD
- Checkout/branch: `feat/react-api-integration`, dirty, not pushed
- Snapshot: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs frontend`, new files intent-added; 23 files) and `HANDOFF-001-SHA256.txt` (25 entries: those 23 plus `scripts/seed-fe.mjs` and `scripts/README.md`)

No Spring, DB or API contract change. The backend used for verification is HEAD unchanged.

## Change summary

| Area | Change |
|---|---|
| API layer | New `api/reservation.ts` (create/list/detail/cancel), `api/healthRecord.ts` (list/create/update/delete), `api/notification.ts` (list/read), and types in `types/reservation.ts`, `healthRecord.ts`, `notification.ts`. The types match the Spring DTOs; JSON field names were checked against live responses (`isRead`, `reservationId`, `healthRecordId`). |
| Shared helpers | `extractErrorMessage` moved into `api/client.ts`, and `formatTime` plus `daysFromToday` into `utils/date.ts`. The two existing copies of each were replaced. |
| ReservationPage (SC-010) | Loads pets, hospital detail and schedules. Dates are chips built from AVAILABLE schedules dated today or later in JST, replacing the free date input; times come from that date's schedules, and BLOCKED slots are disabled. Submits `POST /reservations` and shows the API message on failure, then reloads schedules. The previous page was fully mocked and did not submit. 診療目的 has no API field, so it is optional and prefixed to `memo` as `診療目的: …`, documented in docs/05 SC-010. The button now reads 予約を申請する, since the result is REQUESTED. |
| ReservationHistoryPage (SC-011) | Real list with all five statuses. Filter groups: 予定 = REQUESTED+APPROVED; キャンセル・却下 = CANCELLED+REJECTED. A 詳細 toggle fetches `GET /reservations/{id}` for the memo. Cancel is offered for REQUESTED/APPROVED only, then the list reloads. Removed the 病院詳細 link, which used the reservation id as a hospital id (the summary DTO has no hospitalId), and the no-op レビューを書く button. |
| HealthRecordPage | List, create, edit (PATCH) and delete. The record-type selector (vaccine/medicine/…) did not exist in the model, so the form is now weight / symptom / memo / date. On edit, an empty weight is omitted (PATCH cannot clear it) and empty symptom/memo is sent as `""` to clear it (docs/07 §6-3). On create, empty fields are omitted. |
| NotificationsPage (SC-012) | Real list. Clicking an unread item calls `PATCH /{id}/read`; すべて既読 issues one PATCH per unread item, because there is no bulk endpoint (marked with a `ponytail:` comment). `createdAt` is shown as its JST wall-clock text, not re-interpreted in the device zone. |
| DashboardPage | The next reservation (earliest REQUESTED/APPROVED, today or later) replaces the hardcoded card. The health summary shows the first pet's latest weight, the last five weights as bars scaled min–max, the change versus the previous record, and the two latest symptoms; the `/pets/1/health` link now uses that pet's id. The section is hidden when there is no pet. |
| Logout | The header person icon linked to a non-existent `/profile`. It is now a logout button: confirm, `POST /auth/logout` (errors ignored), `clearAuth()`, navigate to `/login`. |
| Pet list display bug | `PetSummary` returns no breed or gender, so MyPet and Dashboard always showed `— · メス`. They now show the species label (`PET_SPECIES_LABEL`) and the weight. |
| Docs | TODO.md frontend section; AGENTS.md §10 and last-updated line; docs/05 SC-010 input items. |

Not connected (deliberately):
- **AdminReservationPage (SC-013).** Approve/reject/complete endpoints exist, but no endpoint lists reservations for an admin. docs/05 SC-013 requires a list, and docs/07 does not define one. Adding a backend endpoint is outside "connect existing APIs", so it is left for the user to decide; the page is unchanged mock.
- **AdminUserPage.** The backend does not exist.
- **Pet update.** No edit screen exists, and building one was not requested. Note that the frontend `PetUpdateRequest` lacks `petType`, which the backend requires.

## Acceptance evidence

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Type check + bundle | `npm run build` (`tsc -b && vite build`), `frontend/` | Node 24.17.0 | exit 0 |
| Browser E2E (manual) | Vite on :5173 with `VITE_API_BASE_URL_WEB=http://localhost:18081`; boot jar (HEAD) on :18081 with fresh `daipetto_fe_1004`; `scripts/seed-fe.mjs` | In-app Chromium, desktop and 375×812 | All rows below observed |
| Capacitor sync | `npm run cap:sync`, `frontend/` | Capacitor 8.4.1 | exit 0; web assets copied to android and ios (SPM `Package.swift` regenerated, unchanged) |
| Android build | `./gradlew assembleDebug`, `frontend/android/`, with `JAVA_HOME` set to the Android Studio JBR 21 (the default JDK 17 is too old for Capacitor 8) | AGP 8.13.0, Gradle 8.14.3, SDK 36 | BUILD SUCCESSFUL; `app/build/outputs/apk/debug/app-debug.apk`, 4,242,359 bytes, SHA-256 `d78f1167c6dc51cfd59252af3ac381fffb42502e25de9aaf0e0b1dc0cd650f28`; contains `assets/public/assets/index-O81X0W-y.js`, the bundle from the sync build |

Browser observations, logged in as the seeded user through the real login form with the scratch-only fixture:

```text
Dashboard        next reservation "10月6日（火） 09:00 さくら動物病院 - 予約確定", "あと2日" (today 2026-10-04 JST);
                 weight 4.6kg "+0.10kg（前回比）"; symptoms 軽い咳 2日前 / 食欲低下 14日前; pet "猫 · 4.5kg"
Reserve          dates 10-05/10-06/10-07 offered; 10:00 on 10-05 disabled (BLOCKED); confirm shows hospital/pet/
                 purpose/"2026-10-05 09:30〜10:00"; submit -> /reservations, new row 申請中
                 duplicate slot (10-06 09:00, already APPROVED) -> inline "既に予約済みです。" (HTTP 409)
History          approved/rejected/completed/requested rows with labels; 詳細 shows "診療目的: 予防接種\n初めての受診です";
                 cancel -> row becomes キャンセル, cancel button gone
Health record    create 4.75kg + memo -> listed first with date defaulted to 2026-10-04;
                 edit 10-02 record, clear symptom -> DB symptom NULL, weight 4.60 and memo kept; delete -> row removed
Notifications    4 unread; click one -> DB is_read/read_at true for that id only; すべて既読 -> all 4 true
Logout           -> /login, persisted refreshToken null, user's active refresh tokens in DB: 1 -> 0
Console          only error: the intentional 409 from the duplicate booking
```

`window.confirm` was stubbed in the page during the cancel, delete and logout checks so the dialogs would not block automation. After the runs both servers were stopped, `daipetto_fe_1004` was dropped, and the temporary `.claude/launch.json` entries were reverted. `daipetto` and port 8080 were not used.

## Limitations

- The frontend has no automated test framework (no test script in `package.json`). Evidence is the type-checked build plus the manual browser run above. ESLint is listed as a script but is not installed or configured (pre-existing).
- The schedule API does not say which slots are already reserved, so a taken slot is selectable and fails with RESERVATION-001 on submit. A per-slot reserved flag would need a backend change.
- The reservation list keeps the backend order (by id); the UI does not sort it.
- No shared reservation/notification store, so there is no unread badge in the navigation. TODO.md records when to add one.
- The APK is a debug build pointing at `http://10.0.2.2:8080` (emulator to host), from `.env.local`. It was built but not installed on an emulator or device. iOS was synced but not built (Windows host).
- Unrelated to this round: the HospitalMapper `suspend` warning and the Pet PATCH wording/offset-format decisions in TODO.md are unchanged.

## Documentation

TODO.md (frontend wiring section, last-updated), AGENTS.md §10 and last-updated line, docs/05 SC-010 (input items and slot/purpose behaviour, Updated date). No API, ERD, state or security document changed because the backend is untouched.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| None for first round | N/A | N/A | N/A |

## Receiver instructions

Verify the tree against `HANDOFF-001-SHA256.txt` from the repository root, or apply `HANDOFF-001.patch` onto 9c2ae51. Reproduce with `scripts/README.md`. Source write ownership returns to no one with this round. No commit, push or merge is implied.
