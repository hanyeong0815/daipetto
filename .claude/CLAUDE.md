# Daipetto — 詳細コンテキスト（.claude/claude.md）

Claude Code が自動ロードする詳細版共有コンテキスト（ルートの `CLAUDE.md` と併用）。
`docs/` 全13文書と `claude_code_handoff_daipetto.md` の要約、および現状調査結果をまとめる。

最終更新: 2026-07-12（branch: `feat/spring/hospital` 時点）

---

## 1. docs/ ドキュメントインデックス

実ファイル名には Notion エクスポート由来のハッシュが付いている（例: `07_API_Design 363d0…f3.md`）。
参照・更新時は `docs/07_API_Design*.md` のようにプレフィックスで Glob 検索すること。

| No | 文書 | 内容 | 主な更新トリガー |
|---|---|---|---|
| 01 | Project_Overview | 概要・目的・技術スタック | ほぼ固定 |
| 02 | Requirement_Definition | Actor・権限表・BR-001〜005・FR | 機能追加時 |
| 03 | Business_Flow | BF-001〜015 業務フロー | フロー変更時 |
| 04 | System_Architecture | 全体構成・Spring/Django Layer・Scheduler | アーキテクチャ決定時 |
| 05 | Screen_Design | SC-001〜015 画面設計 | 画面変更時 |
| 06 | ERD | 全10テーブル定義・Status・Index | **DB変更時（必須）** |
| 07 | API_Design | 全API仕様・ErrorCode・Role権限 | **API変更時（必須）** |
| 08 | State_Design | 状態値・状態遷移ルール・禁止遷移 | **Status/遷移変更時（必須）** |
| 09 | Security_Design | JWT・CORS・Validation・Rate Limit | 認証認可変更時 |
| 10 | Development_Environment | バージョン・Docker・Git運用 | 環境・バージョン変更時 |
| 11 | Test_Strategy | テスト観点ID（AUTH-T/ROLE-T/RSV-T等） | テストシナリオ変更時 |
| 12 | Trouble_Shooting | 障害事例と対応 | 新規トラブル解決時 |
| 13 | Retrospective | 振り返り | 随時 |

---

## 2. docs 未完成箇所（2026-07-12 調査結果）

作業時にこれらの箇所に依存しないこと。補完する場合は変更管理ルールに従う。

- **02**: 機能要件が FR-AUTH-001/002・FR-PET-001・FR-HEALTH-001 のみ。病院・予約・通知・分析系の FR が未記載。handoff §5 が想定する「BR別ファイルのフォルダ構成」とも不一致（実体は単一ファイル）。
- **03**: BF-004（ペット情報更新）・BF-007（病院検索）は一覧に載っているが詳細セクションが存在しない。
- **05**: 「4. 共通UI設計方針」セクションが重複している。
- **06**: pets テーブルに `breed` / `neutered` / `microchip_number` が未反映。反映済みの記述は未マージの `feat/spring/pet-fields` ブランチにのみ存在する。
- **07**: 11-2 病院更新・11-3 病院停止が「（未実装）」。予約枠登録 API（`POST /api/v1/hospitals/{hospitalId}/schedules`）は文書自体に未記載＝設計ギャップ（実装前に設計追記が必要）。
- **09**: 5-1 のコードブロックが ` ```mermaid ` 誤記（BCryptPasswordEncoder）。
- **10**: 6-2「Python Version」が空欄。5-9・6-3 のコードブロックも mermaid 誤記。5-6 Directory構成・5-7 Layer構成が旧設計（`backend-spring/`・Controller→Service→Repository）のままで、実際のヘキサゴナル構造（§3参照）と不一致。
- 全文書の Status が「Draft」のまま。多くの Updated が 2026-05-17 で止まっている。
- handoff §24 のプロンプト例は `docs/00_Claude_Code_Handoff.md` を参照しているが、実体はルートの `claude_code_handoff_daipetto.md`。

---

## 3. Spring API アーキテクチャ（変更禁止）

```text
Controller → UseCase Interface → UseCase Service → Domain Model
→ Repository Port → Persistence Adapter → MapStruct Mapper
→ JPA Entity → JpaRepository → PostgreSQL
```

実パッケージ構造（`spring-api/src/main/java/koh/portfolio/springapi/`）:

```text
common/          exception（ErrorCode, CustomException, Preconditions, GlobalExceptionHandler）
                 response（ApiResponse）, time（ServerTime）
domain/{ドメイン}/    model / exception / port
application/{ドメイン}/ dto / usecase / service
infrastructure/  mapper（DomainEntityMapper）, persistence/{ドメイン}, security（jwt, handler, cors）
presentation/{ドメイン}/ Controller
```

※ 過去に存在した `application/pet/suervice/` の typo は現ブランチでは修正済み（`service`）。

実装規則（handoff §7〜§13 要約）:

1. Domain と JPA Entity は必ず分離。Domain は JPA 非依存。
2. UseCase は interface、Service が実装体。単純ドメインは1 Serviceが複数UseCaseをimplements可、複雑ドメイン（Reservation等）はUseCase別Service分離。
3. Domain ↔ Entity 変換は MapStruct（`DomainEntityMapper<DOMAIN, ENTITY>` を継承）。
4. Business error は `ErrorCode`(enum, `code()`必須) + `CustomException` + `Preconditions.validate(...)`。
5. API response は `ApiResponse`。`GlobalExceptionHandler` は CustomException / AccessDeniedException / MethodArgumentNotValidException / Exception を処理。
6. `userId` は DTO に入れず `Authentication.getPrincipal()` から `Long` で取得。
7. 重要な DB 更新は dirty checking より明示的 `@Modifying` update query を優先。
8. Role 検証は `@PreAuthorize("hasAuthority('ROLE_...')")`（SecurityConfig の `authenticated()` は認証のみ）。
9. owner チェックは `Preconditions.validate` パターン（`UpdatePetService` 参照）。

---

## 4. ErrorCode 一覧（実装済み・設計済み）

| Code | 内容 | HTTP |
|---|---|---|
| AUTH-001 | 認証に失敗しました。 | 401 |
| AUTH-002 | 利用停止中のアカウントです。 | 403 |
| AUTH-003 | 無効なRefresh Tokenです。 | 401 |
| AUTH-004 | 期限切れのRefresh Tokenです。 | 401 |
| AUTH-005 | アクセス権限がありません。 | 403 |
| AUTH-006 | サポートされていないTokenです。 | 401 |
| AUTH-999 | 認証関連エラーです。 | 500 |
| USER-001 | 重複メール | 409 |
| USER-002 | バリデーションエラー | 400 |
| HOSPITAL-001 | 存在しない病院 | 404 |
| HOSPITAL-002 | 存在しない予約枠、または指定したhospitalIdに属さない予約枠 | 404 |
| RESERVATION-001〜005 | 重複予約/予約不可時間/BLOCKED/他人ペット/過去日時（未実装・設計済み） | - |

エラーメッセージは日本語。新ドメイン追加時は `{DOMAIN}-{連番}` 形式で採番する。

---

## 5. Enum / Status / 状態遷移

| 対象 | 値 |
|---|---|
| Role | ROLE_USER / ROLE_HOSPITAL_ADMIN / ROLE_SYSTEM_ADMIN |
| users.status, hospitals.status | ACTIVE / SUSPENDED |
| hospital_schedules.status | AVAILABLE / BLOCKED |
| reservations.status | REQUESTED / APPROVED / REJECTED / COMPLETED / CANCELLED |
| vaccinations.status | SCHEDULED / COMPLETED / CANCELLED |
| NotificationType | RESERVATION_APPROVED / RESERVATION_REJECTED / VACCINATION / TREATMENT_COMPLETED |

予約状態遷移（許可される遷移のみ。Service層で制御）:

| 現在 | 次 | 実行者 |
|---|---|---|
| REQUESTED | APPROVED / REJECTED | HOSPITAL_ADMIN |
| REQUESTED | CANCELLED | USER |
| APPROVED | COMPLETED | HOSPITAL_ADMIN |
| APPROVED | CANCELLED | USER |

禁止遷移: COMPLETED→REQUESTED, CANCELLED→APPROVED, REJECTED→APPROVED, COMPLETED→CANCELLED。
同一 schedule_id に REQUESTED / APPROVED は1件のみ。承認/却下/完了時は対応する Notification を生成する（08_State_Design 6-6）。

---

## 6. DB / Flyway

- DB 変更は Flyway migration のみ。手動変更禁止。論理削除（deleted_at）が基本。
- 現ブランチ適用済み: `V1__create_users` / `V2__create_refresh_tokens` / `V3__create_pets` / `V5__create_hospitals` / `V6__create_hospital_schedules`
- **V4 は未マージの `feat/spring/pet-fields` が使用済み**（`V4__alter_pets_add_columns.sql`）。新規 migration は V7 以降を使い、採番前に全ブランチの番号衝突を確認すること。
- 未作成テーブル: reservations / health_records / vaccinations / notifications / hospital_admins（定義は 06_ERD 参照）

---

## 7. テスト規則（要点）

- メソッド名: 英語 / `@DisplayName`: 日本語 / コメント最小限
- Spring Boot 3.3.13 → `@MockBean` を使用（`@MockitoBean` 禁止）
- Controller テスト: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` + `@Import(GlobalExceptionHandler.class)`。JwtAuthenticationFilter が Context 生成失敗を起こす場合は excludeFilters で除外
- Controller の全 UseCase 依存を漏れなく `@MockBean` にする
- principal が Long の場合、テストでも `UsernamePasswordAuthenticationToken(1L, null, authorities)` を使う（`.with(user(...))` は不可）
- 検証コマンド: `./gradlew clean test`（spring-api/ で実行）

---

## 8. 開発環境（実バージョン）

| 区分 | 値 |
|---|---|
| Spring | Java 17 / Spring Boot 3.3.13 / Gradle 8.14.4 / Flyway 10.20.x / MapStruct 1.5.5.Final |
| Frontend | React 18.3.1 / TS 5.7.3 / Vite 6.4.3 / Node 24.17.0 / Tailwind 3.4.19 / Zustand 5.0.14 / Axios 1.18.1 / React Router 6.30.4 |
| Django | Python 3.12.10 / DRF 3.15.2（**未着手**） |
| DB | PostgreSQL 16.9（DB名/User: daipetto） |
| Port | frontend 5173 / spring-api 8080 / django-api 8000 / postgres 5432 |

起動: `docker compose up -d`（postgres）→ `./gradlew bootRun` / `npm run dev`

---

## 9. 進捗と意図的未実装（練習用）

**完了**: 会員登録 / ログイン / JWT / Refresh Token 再発行（rotation） / ログアウト / `GET /users/me` / Pet CRUD 全部 / Hospital（Create・List・Detail・ScheduleList）/ Frontend 全画面 SC-001〜015（auth・pet のみ実API連動済み）

**練習用にあえて未実装（ユーザー本人が実装する。勝手に実装しないこと）**:
- `PATCH /api/v1/admin/hospitals/{id}`（病院更新。PUT→PATCH 整合性は要再確認）
- `PATCH /api/v1/admin/hospitals/{id}/suspend`（病院停止）
- `POST /api/v1/hospitals/{hospitalId}/schedules`（予約枠登録。docs 未記載のため設計から必要）
- 予約枠 AVAILABLE/BLOCKED 切替 API

**未着手**: Reservation API / HealthRecord API / Vaccination API / Notification API / ワクチン通知 Scheduler / django-api 全体 / Frontend の病院・予約・健康記録・通知・管理者画面の実API連動 / Protected Route・ロールガード

**未マージブランチ**: `feat/spring/pet-fields`（breed 等の Pet 列追加 + docs 06/07 更新）, `feat/spring/hospital`（現ブランチ）

---

## 10. 作業運用ルール

1. **練習用ギャップ**: 新ドメイン実装時は 1〜2 機能を意図的に未実装で残し、TODO.md の「あえて未実装のまま残した項目（練習用）」と docs に「（未実装）」を記載する（ユーザーの学習方針）。
2. **変更後の同期**: `claude_code_handoff_daipetto.md` §22（進捗）→ `TODO.md` → 該当 `docs/` の順に更新する。詳細手順は `.claude/skills/docs-sync/SKILL.md`。
3. **サブエージェント**: `.claude/agents/` に spring-api-dev / frontend-dev / django-api-dev / docs-sync を定義済み。領域を跨ぐ大きい作業で活用する。
4. **スキル**: 新ドメイン追加は `/spring-new-domain`、テスト作成は `/spring-test`、文書同期は `/docs-sync` を使用する。
