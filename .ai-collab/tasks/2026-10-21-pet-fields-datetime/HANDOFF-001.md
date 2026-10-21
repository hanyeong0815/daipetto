# Handoff 001: 2026-10-21-pet-fields-datetime

- Author / implementer: Claude Code
- Publication: ready
- Timestamp: 2026-10-21T23:35+09:00
- Task specification: TASK.md
- Recipient: Codex
- Requested action: review only
- Source base SHA: `fe8aa0e` (branch `feat/spring/pet-fields-datetime`, created from `feat/spring/vaccination`, whose previous round was ACCEPTED)
- Merge in progress: `git merge --no-commit --no-ff feat/spring/pet-fields` (MERGE_HEAD `b83aef4`). The accepted snapshot will be committed as that merge commit.
- Source head SHA: none — uncommitted working tree
- Diff basis: `git diff HEAD` (staged merge result + unstaged follow-up edits)
- Checkout/branch and dirty state: `E:\workspace\daipetto`; new files intent-added; unrelated untracked `.vscode/` excluded
- Dirty snapshot: `HANDOFF-001.patch` (`git diff HEAD -- AGENTS.md TODO.md docs .agents spring-api frontend`) and `HANDOFF-001-SHA256.txt` (changed files + this task's `scripts/` + the patch)

## Change summary

Merge of `feat/spring/pet-fields` (July, by the user with Claude): `Pet`/`PetEntity`/DTOs/services gain `breed`, `neutered`, `microchipNumber`; `PetSummary` gains `breed`; docs 06/07 pet sections. It auto-merged without conflicts. Follow-up edits made on top:

| Area | Change | Why |
|---|---|---|
| Migration number | `V4__alter_pets_add_columns.sql` → `V13__alter_pets_add_columns.sql` (content unchanged) | V4..V12 are taken on this line |
| `PetJpaRepository.updateProfile` / adapter | also writes `breed`, `neutered`, `microchipNumber` | the merged branch predated the conditional update (HANDOFF-002 of 2026-10-03 flagged this); without it edits to the new fields are silently dropped |
| `V14__make_pet_gender_nullable.sql` | `ALTER COLUMN gender DROP NOT NULL` | docs 02/05/06 define gender as optional and `CreatePetRequest.gender` has no `@NotNull`, but V3 created it NOT NULL, so creating without gender returned 500. `UpdatePetRequest.gender` lost its `@NotNull` for the same reason. |
| `CreatePetRequest` / `UpdatePetRequest` | `name @Size(max=50)`, `breed @Size(max=100)`, `weight @PositiveOrZero @Digits(3,2)` | column limits (VARCHAR(50)/(100), DECIMAL(5,2)); previously 500 at the DB |
| `common/time/LocalDateTimeJsonSerializer` (`@JsonComponent`) | every response `LocalDateTime` → `atZone(Asia/Tokyo)`, second precision, `ISO_OFFSET_DATE_TIME` | user decision: docs/07 §2-7 `+09:00` format. Domain/DB stay `LocalDateTime` (JST). Picked up by `@WebMvcTest` slices too. Response fields affected: `createdAt` in pet detail, hospital, notification DTOs. |
| Tests | 9 test files updated for the new `Pet` constructor; merged update test moved from `save` to `updateProfile`; `PetPersistenceAdapterTest` asserts the 3 columns are written; `PetControllerTest` adds validation (5 cases) and create-without-gender; `NotificationControllerTest` asserts `2026-05-20T10:00:30+09:00` from `10:00:30.123` | — |
| Frontend | `PetRegisterPage` is now a shared create/edit form: `/pets/:petId/edit` keyed by pet, prefills from `GET /pets/{id}`, shows a spinner until loaded and an error + back button if loading fails (it never shows an empty form that would overwrite everything), and sends the full body. Gender has 「不明」 (null). `name maxLength 50`, `breed 100`, weight `step 0.01 / max 999.99`. `petApi.update` maps `species → petType` (it previously sent `species`, which the API ignores). `PetUpdateRequest = PetCreateRequest`. Detail 「編集」 is a link. Gender label shows 「—」 for null. The list shows breed. Title 「ペット情報の編集」. | user decision: full replacement |

Pet update is a full replacement, so nothing is merged from the read row and no locked read is needed (AGENTS §3 rule 5 second sentence does not apply).

## Acceptance evidence (all executed by Claude Code, 2026-10-21)

| Criterion | Command / directory | Environment | Outcome |
|---|---|---|---|
| Whole suite | `spring-api/`: `./gradlew clean test` then `./gradlew test` after the last Java edit | JDK 17, H2 | exit 0 — 261 tests, 0 failures/errors/skips (256 before) |
| Frontend build | `frontend/`: `npm run build` after the last edit | Node 24.17.0 | exit 0 |
| AC-01..AC-05 on PostgreSQL | `scripts/pet-datetime-acceptance.mjs` against the bootJar (built after the last Java edit) | docker `postgres:16.9`, fresh DB `daipetto_pet_1021`, port 18081 | 17/17 (list in `scripts/README.md`); Flyway history exactly 1..14 |
| Upgrade V12 → V14 | jar with `--spring.flyway.target=12`, insert a user + pet (gender MALE, weight 8.00) via psql, restart | scratch DB `daipetto_pet_up` | "Successfully applied 2 migrations … v14"; pet row `Old/MALE/8.00/NULL/false/NULL`; `gender` is_nullable = YES |
| AC-06 browser | Vite 5173 → 18081, fixture session injected as refresh token (no password typed) | built-in browser | list shows 「猫 · Mix」. Detail shows breed, 不妊手術済み, microchip. 「編集」 opened `/pets/2/edit` prefilled with every field. Breed changed + 「不明」 saved; DB `Momo3/NULL/5.25/Scottish Fold Mix/true/392000000000002`, so the other fields were preserved because the form sends all of them. Detail shows 「Scottish Fold Mix · —」. Register with 「不明」 stored gender NULL. Notifications page shows 「2026-10-21 23:59」 for an offset value. |

All scratch databases were dropped and the temporary launch configuration reverted.

## Limitations

- Full replacement means a client that sends a partial body clears fields; documented in docs/07 §5-4 and enforced in the UI by prefilling. A concurrent edit by two tabs is last-writer-wins for the whole profile (accepted by the user's decision; deletion is still never undone).
- `CreatePetRequest.gender` was already optional; existing rows keep their values. Pets created before V14 always have a gender.
- The offset serializer is applied by Jackson only; any `LocalDateTime` turned into a string manually would not get it (none found: `grep LocalDateTime application/*/dto` lists pet, hospital, notification DTOs only).
- docs/05 previously said 「ペット名 最大20文字」; it now says 50 to match the column. Existing names up to 50 remain valid.
- The photo picker on the pet form is still a placeholder (image upload undecided, `pets.image_url` not implemented).
- No Android/iOS runtime check.

## Documentation

- docs/02: FR-PET-001 inputs and limits. A leak of these rows into FR-HEALTH-001 during editing was caught and removed.
- docs/05: SC-005 inputs/validation, and the edit mode via `/pets/:petId/edit`; SC-006 edit row.
- docs/06: inventory row; `gender` VARCHAR(10) NULL with a V14 note.
- docs/07: §2-7 offset rule; §5-2 response example; §5-3 validation; §5-4 full replacement; notification example with `+09:00`.
- docs/11: PET-T006..T008, COMMON-T001.
- docs/12: §3-15 gender NOT NULL / missing validation.
- TODO.md: pet-fields merged; two pending decisions resolved; pet edit screen.
- AGENTS.md: §7 V13/V14, next V15, unmerged warning removed; §8 261; §10 new status, unmerged-branch line, known gaps.

## Response to previous review

| Finding ID | Fixed / disputed / deferred | Evidence or revision | Remaining action |
|---|---|---|---|
| None for first round | N/A | N/A | N/A |

## Receiver instructions

Verify `HANDOFF-001-SHA256.txt` from the repository root. `git status` shows a merge in progress; review the working tree against `HEAD` (`fe8aa0e`). If Gradle cannot use the default user home, `spring-api/.gradle/review-home` from your earlier runs is available (`--offline`). PostgreSQL steps are in `scripts/README.md`; use only new scratch databases and a port other than 8080. Write findings to `REVIEW-001.md` in this directory.
