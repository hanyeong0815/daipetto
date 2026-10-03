# Daipetto — 作業TODOリスト

最終更新: 2026-09-23

---

## ✅ 完了済み

### Spring API
- [x] PostgreSQL Docker 起動
- [x] Spring Boot ↔ PostgreSQL 接続
- [x] Flyway migration 確認
- [x] 会員登録 (`RegisterUseCase` / `RegisterService`)
- [x] ログイン (`LoginUseCase` / `LoginService`)
- [x] JWT 発行・検証 (`JwtProvider` / `JwtAuthenticationFilter`)
- [x] Refresh Token 再発行 (`RefreshTokenUseCase` / `RefreshTokenService`)
- [x] ログアウト (`LogoutUseCase` / `LogoutService`)
- [x] ユーザープロフィール取得 `GET /api/v1/users/me`
- [x] ペット CRUD API (`POST / GET list / GET detail / PATCH / DELETE /api/v1/pets`)
  - owner チェックを `Preconditions.validate` パターンで統一済み
- [x] Hospital・HospitalSchedule・HospitalBusinessHours CRUD API（2026-08-02〜08-09実装、詳細は本ファイル下部参照）
- [x] Reservation API（2026-08-23実装、2026-09-13に予約却下`reject`追加で完成）
  - `V7__create_reservations.sql`、`domain/reservation`・`application/reservation`・`infrastructure/persistence/reservation`・`presentation/reservation` 一式
  - 実装: `POST /api/v1/reservations`（申請）・`GET /api/v1/reservations`（一覧）・`GET /api/v1/reservations/{id}`（詳細）・`PATCH /api/v1/reservations/{id}/cancel`（キャンセル）・`PATCH /api/v1/admin/reservations/{id}/approve`（承認）・`PATCH /api/v1/admin/reservations/{id}/complete`（診療完了）・`PATCH /api/v1/admin/reservations/{id}/reject`（却下、ユーザー実装＋Claude Codeレビュー）
  - `ReservationErrorCode`: RESERVATION-001〜005（`docs/07_API_Design` §9-3準拠）に加え、実装時に006（予約が見つからない）・007（予約者本人ではない）・008（不正な状態遷移）を新規追加
  - 状態遷移（承認・キャンセル・完了・却下）は `Reservation` ドメインモデル内のメソッドで制御（`Hospital.suspend()`と同じ不変オブジェクトパターン）
  - テスト37件追加（Service 28件・Mapper 2件・Controller 7件）、`./gradlew clean test`で140件全green
  - **reject実装レビューで発見・修正したバグ（2026-09-13, Claude Code）**: `Reservation.reject()`がREQUESTED/APPROVED/CANCELLEDの3状態から却下可能になっていた（`docs/07_API_Design` §9-6・`AGENTS.md` §6の「REQUESTED→REJECTEDのみ」に反する）。REQUESTED限定に修正し、APPROVED/CANCELLED/COMPLETED/REJECTED各状態からの却下を拒否する回帰テストを追加。`ReservationAdminControllerTest`の`reject_reservation_success`の`@DisplayName`が「診療完了成功」のコピペ違いだったのも修正
  - 却下理由（`reason`）は実装せず: `reservations`にカラムが無く設計ギャップだったため、docsからRequest例を削除しスコープ外と明記

- [x] Codexコードレビュー指摘（REVIEW-001、2026-09-13）の対応 — Claude Code
  - R-01 Refresh Token消費の非原子性: 条件付き失効の**更新件数が1件**の呼び出しのみ後続発行を許可（0件はAUTH-003）。`RefreshTokenRepository.revokeByToken`をintに変更
  - R-02 同一秒のRefresh Token重複: `jti`(UUID)を付与（`token`のUNIQUE制約違反による500を回避）
  - R-03 二重予約: `V8__add_reservations_active_schedule_unique_index.sql`（部分UNIQUE）追加＋制約違反をRESERVATION-001に変換（`saveAndFlush`で検知）
  - R-04 終了状態の上書き: 承認・却下・完了・キャンセルを「読み取り時点の状態」を条件にした条件付きUPDATEへ変更。0件更新はRESERVATION-008。`RejectReservationService`に`@Transactional`追加
  - R-05 SUSPENDED病院への予約: `CreateReservationService`で病院状態を検証、`RESERVATION-009`新規追加
  - R-06 論理削除ペットで予約一覧が壊れる: `PetRepository.findByIdIncludingDeleted`を追加し履歴表示に使用
  - R-07 Refresh TokenをBearerに使うとフィルターで例外: `type` claimでAccess Token専用に検証（`validateAccessToken`）
  - R-08 リフレッシュ失敗時に待機リクエストが未解決: キューに`reject`も保持して全waiterをsettle
  - テスト17件追加（計157件green）。詳細は `.ai-collab/tasks/2026-09-13-implementation-review/HANDOFF-002.md`
  - [ ] **残課題**: 実PostgreSQLでの同時実行テスト（R-01/R-03/R-04）は未実施。モックテストでは競合を検証できない（`docs/12` §3-11）
  - [x] **R-01の残り（REVIEW-002指摘、2026-09-23対応）**: refreshとlogin/logoutの一括revokeが直列化されておらず、ログアウト後も後続Tokenが生き残る（＝セッションが残る）問題。login/refresh/logoutの3操作すべてでTokenを触る前に`UserRepository.lockForSessionUpdate`（ユーザー行の`SELECT ... FOR UPDATE`）を取得して直列化。保留していた同時ログインの残課題もこれで解消
  - [x] REVIEW-002の非ブロッキング所見: `ReservationPersistenceAdapter`が全ての整合性違反をRESERVATION-001に変換していた点を、`uq_reservations_active_schedule`違反のみに限定（他はそのまま伝播）
  - [ ] **残課題**: 実PostgreSQLでの同時実行検証（refresh×logout / refresh×login / 同時ログイン）は、Docker未起動のため2026-09-23時点で未実施。検証スクリプトは用意済み

### Frontend
- [x] プロジェクト初期構築 (React 18 + TypeScript + Vite + Tailwind CSS)
- [x] デザイントークン設定 (`tailwind.config.ts`)
- [x] 共通レイアウト (`MainLayout` / `DetailLayout` / `BottomNav` / `TopAppBar`)
- [x] 全画面実装 (SC-001 〜 SC-015)
  - SC-001 ログイン
  - SC-002 会員登録
  - SC-003 ダッシュボード
  - SC-004 マイペット一覧
  - SC-005 ペット登録
  - SC-006 ペット詳細
  - SC-007 健康記録
  - SC-008 病院検索
  - SC-009 病院詳細
  - SC-010 予約（3ステップ）
  - SC-011 予約履歴
  - SC-012 通知
  - SC-013 予約管理（病院管理者）
  - SC-014 病院情報管理（病院管理者）
  - SC-015 ユーザー管理（システム管理者）
- [x] UI テキストを日本語に統一（「大事なペットをもっと大切に。」強調表示含む）
- [x] Zustand 導入 (`authStore` / `petStore`)
- [x] Axios 導入 + JWT インターセプター + 401 自動リフレッシュ
- [x] Spring API との疎通実装 (auth / pet エンドポイント)
- [x] `html lang="ja"` 設定
- [x] `docs/10_Development_Environment` バージョン更新

---

## 🚧 未実装 — Spring API

### Pet テーブル不足カラム追加（実装済み・未マージ — branch: `feat/spring/pet-fields`）
> 2026-07-05 実装済み。`breed` は自由入力文字列で確定（マスタテーブル無し）。**develop へのマージ待ち**

- [x] `V4__alter_pets_add_columns.sql` — `breed`, `neutered`, `microchip_number` カラム追加
- [x] `PetEntity` / `Pet` ドメイン / `PetMapper` / DTO / Service 反映
- [x] docs/06_ERD・07_API_Design 更新（同ブランチ内）
- [ ] **マージ時の必須作業**: migration 番号衝突の解消 — 現ブランチの `V4` は hospitals が使用中のため、`V4__alter_pets_add_columns.sql` を V8 以降へリナンバーする

### Hospital API（branch: `feat/spring/hospital`）
- [x] `Hospital` / `HospitalSchedule` ドメイン・エンティティ・テーブル作成
  - 実際のマイグレーション番号: `V4__create_hospitals.sql` / `V6__create_hospital_schedules.sql`（Pet列追加が未マージのままV4を使用しているためズレ、V5はHospitalBusinessHoursが使用）
- [x] `GET /api/v1/hospitals` — 病院一覧（keyword・area検索対応、ACTIVEのみ）
- [x] `GET /api/v1/hospitals/{id}` — 病院詳細
- [x] `POST /api/v1/admin/hospitals` — 病院登録（`docs/07_API_Design`のRole別権限表に合わせROLE_SYSTEM_ADMIN限定。パスはdocs通り`/admin`配下）
- [x] `GET /api/v1/hospitals/{hospitalId}/schedules` — 病院予約枠一覧
- [x] Hospital/HospitalSchedule 関連テスト（Create/List/Detail/ScheduleList分）

**あえて未実装のまま残した項目（練習用、ユーザーが実装中）:**
- [x] `PATCH /api/v1/admin/hospitals/{id}` — 病院情報更新（実装済み、`hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')`）
- [x] `PATCH /api/v1/admin/hospitals/{id}/suspend` — 病院停止（実装済み、`ROLE_SYSTEM_ADMIN`）
- [x] `POST /api/v1/admin/hospitals/{hospitalId}/schedules` — 予約枠の個別手動登録（ユーザー実装。`@Valid`・`existsById`チェック追加済み）
- [x] 予約枠のAVAILABLE/BLOCKED切り替え — `PATCH .../schedules/{scheduleId}/block`・`.../unblock`（Claude Code実装、2026-08-02。`BlockHospitalScheduleService`/`UnblockHospitalScheduleService`、`HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND`追加、`docs/07_API_Design` 8-5・8-6追記、テスト10件green）
  - 状態遷移は`Hospital.suspend()`と同じ「不変オブジェクト・新インスタンス返却」パターン
  - schedule.hospitalIdとpath変数のhospitalIdが一致しない場合もHOSPITAL-002（他病院の予約枠を弄れないようにするガード。Pet の owner check に相当するが hospital_admins が無いので厳密な権限チェックではない点に注意）
  - **予約による自動BLOCKEDは行わない**（`06_ERD` §12業務制約、§18参照）
- [x] Hospital/HospitalSchedule 全体テスト整備（Claude Code、2026-08-02）。テスト作成中に発見して同時に修正したバグ:
  - `HospitalAdminControllerTest`の`@MockBean`漏れ（`UpdateHospitalUseCase`/`SuspendHospitalUseCase`未Mock → 3件失敗）を修正
  - `CreateHospitalScheduleResponse.HospitalScheduleId`（大文字始まり）→`scheduleId`に修正（docsと不一致だった）
  - **`HospitalScheduleService`の予約枠登録で`Preconditions.validate(!hospitalRepository.existsById(hospitalId), ...)`が条件反転していたバグ** — 存在する病院への登録が誤ってHOSPITAL-001で弾かれ、存在しない病院への登録はDB制約違反まで素通りしていた。`!`を削除して修正
  - Hospital(Update/Suspend/Create) + HospitalSchedule(Create/Block/Unblock/List) 全メソッドにService・Controllerテスト追加、`./gradlew test`で84件全green

### 病院営業時間管理 API（branch: `feat/spring/hospital`、2026-08-09実装）
- [x] `hospital_business_hours` テーブル作成（`V5__create_hospital_business_hours.sql`、曜日単位、`day_of_week`は`java.time.DayOfWeek`のname()と一致）
  - UNIQUE(hospital_id, day_of_week)。レコードが無い曜日 = 休診（フラグは持たない）
  - ユーザー作成のドラフトにバグあり（Claude Codeが発見・修正）: `close_time`のNOT NULL/`break_end_time`のNULL指定が逆、UNIQUE制約の欠落、FK制約名のコピペミス、Entityフィールド名`OpenTime`/`CloseTime`の大文字始まり、`dayOfWeek`が`String`型（`DayOfWeek`enumに変更）
- [x] `POST /api/v1/admin/hospitals/{hospitalId}/business-hours` — 営業時間登録（`docs/07_API_Design` §8-7、ROLE_HOSPITAL_ADMIN・ROLE_SYSTEM_ADMIN）
- [x] `GET /api/v1/hospitals/{hospitalId}/business-hours` — 営業時間一覧取得（§8-8、認証のみ）
- [x] `PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` — 営業時間更新（§8-9。`dayOfWeek`は変更不可、変更する場合は削除して登録し直す設計）
- [x] `DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` — 営業時間削除（§8-10。レコード削除＝当該曜日休診）
- [x] `HOSPITAL-003`（存在しない営業時間）・`HOSPITAL-004`（曜日重複登録）エラーコード追加
- [x] テスト16件追加（Service 11件 + Controller 5件）、`./gradlew test`で全100件green

### 予約枠自動生成バッチ（設計決定、未実装 — 2026-08-02）
> ユーザーとの設計議論の結論。`hospital_business_hours`管理APIは実装済みだが、これを元に`hospital_schedules`を自動生成するScheduler本体は未着手

- [ ] 予約枠自動生成Scheduler（`@Scheduled`、`04_System_Architecture` §14参照）
  - `hospital_business_hours`を元に、翌月分など一定期間の`hospital_schedules`を`slot_duration_minutes`単位で分割生成
  - 休憩時間帯も同じ単位で分割し`BLOCKED`として生成（行を作らず空白にする方式は採らない — 画面上「休憩中」と表示できるようにするため）
  - 既に生成済みの期間は再生成しない（手動BLOCKEDの上書き防止）

### Reservation API（2026-08-23実装、Claude Code）
- [x] `Reservation` ドメイン・エンティティ・テーブル作成 (`V7__create_reservations.sql`。V5/V6はHospitalBusinessHours/HospitalScheduleが使用済みのためV7から)
- [x] 予約ステータス設計: `REQUESTED → APPROVED → COMPLETED`／`REQUESTED → REJECTED`／`REQUESTED・APPROVED → CANCELLED`（`docs/06_ERD` §12・`docs/08_State_Design` §6・ルート`AGENTS.md` §6 準拠。旧記載の `PENDING → CONFIRMED` は誤りだったため修正）
- [x] `POST /api/v1/reservations` — 予約申請（USER、docs/07_API_Design §9-3）
- [x] `GET /api/v1/reservations` — 予約一覧（ユーザー別、§9-1）
- [x] `GET /api/v1/reservations/{id}` — 予約詳細（§9-2）
- [x] `PATCH /api/v1/reservations/{id}/cancel` — 予約キャンセル（USER、§9-4）
- [x] `PATCH /api/v1/admin/reservations/{id}/approve` — 予約承認（HOSPITAL_ADMIN、§9-5）
- [x] `PATCH /api/v1/admin/reservations/{id}/complete` — 診療完了（HOSPITAL_ADMIN、§9-7）
- [x] RESERVATION-001〜008 ErrorCode 追加（001〜005はdocs/07_API_Design §9-3準拠、006〜008は実装時に新規追加。ルート`AGENTS.md` §5参照）
- [x] `PATCH /api/v1/admin/reservations/{id}/reject` — 予約却下（HOSPITAL_ADMIN/SYSTEM_ADMIN、docs/07_API_Design §9-6。2026-09-13、ユーザー実装＋Claude Codeレビューで完成。詳細は上部の完了済みセクション参照）
- [x] Reservation 関連テスト（Service 28件・Mapper 2件・Controller 7件、計37件）

### Notification API
- [ ] `Notification` ドメイン・エンティティ・テーブル作成
- [ ] `GET /api/v1/notifications` — 通知一覧
- [ ] `PATCH /api/v1/notifications/{id}/read` — 既読
- [ ] 予約確定・リマインダー時の通知生成ロジック
- [ ] 予防接種リマインダー Scheduler

### Health Record API
- [ ] `HealthRecord` エンティティ・テーブル作成
- [ ] `POST /api/v1/pets/{petId}/health-records`
- [ ] `GET /api/v1/pets/{petId}/health-records`

---

## 🚧 未実装 — Django Analysis API

### 環境構築
- [ ] Django プロジェクト初期構築 (`django-api/`)
- [ ] `requirements.txt` 整備（Django REST Framework / psycopg2 / pandas / scipy）
- [ ] Docker Compose への Django サービス追加
- [ ] PostgreSQL 接続設定（Spring と同一 DB or 分離 DB の選択）
- [ ] CORS 設定（Frontend からのアクセス許可）

### HealthRecord 分析 API
- [ ] `GET /api/v1/analysis/pets/{petId}/weight` — 体重推移データ（期間指定対応）
- [ ] `GET /api/v1/analysis/pets/{petId}/health-score` — 健康スコア算出（体重変化・症状頻度ベース）
- [ ] `GET /api/v1/analysis/pets/{petId}/symptoms/summary` — 症状統計（頻度・傾向）

### 予防接種リマインダー分析
- [ ] `GET /api/v1/analysis/pets/{petId}/vaccination/next` — 次回接種推奨日の算出

---

## 🚧 未実装 — Frontend（API 連動）

### Hospital API 連動（2026-08-09 実装、Claude Code）
- [x] 病院検索 (`HospitalSearchPage`) — Hospital API 連動。distance/rating/reviews/specialtyはバックエンドに存在しないため削除
- [x] 病院詳細 (`HospitalDetailPage`) — Hospital + HospitalBusinessHours API 連動。doctors/reviewsはバックエンドに存在しないため削除、診療時間は曜日別に表示
- [x] 管理者画面 (`AdminHospitalPage`) — Hospital（Create/Update/Suspend）+ HospitalSchedule（Create/Block/Unblock）+ HospitalBusinessHours（Create/Update/Delete）全連動
  - `hospital_admins`未実装のため「自分の病院」を判定できず、一覧から選択する方式で暫定対応
- [x] `hospitalStore.ts` — 病院一覧・詳細・予約枠・営業時間（読み取り用）
- [x] `api/hospital.ts` — 全Hospital関連API（公開4 + 管理者6）のラッパー
- [x] プラットフォーム別APIベースURL — `Capacitor.getPlatform()`で実行時に自動判定（Web/Android/iOS）、`.env`の手動書き換えが不要に

### 実 API 接続が必要な画面（残り）
- [ ] 予約 (`ReservationPage`) — Reservation API 連動（バックエンド未実装のため連動不可）
- [ ] 予約履歴 (`ReservationHistoryPage`) — Reservation API 連動
- [ ] 健康記録 (`HealthRecordPage`) — HealthRecord API 連動
- [ ] 通知 (`NotificationsPage`) — Notification API 連動
- [ ] 管理者画面 (`AdminReservationPage` / `AdminUserPage`) — 各API連動（バックエンド未実装のため連動不可）

### 状態管理追加
- [ ] `reservationStore.ts` — 予約一覧・詳細
- [ ] `notificationStore.ts` — 通知・未読数

### 認証フロー（2026-08-09 実装、branch: `fix/auth-session-issues`）
- [x] Protected Route 実装（`components/auth/ProtectedRoute.tsx`。未ログイン → `/login` にリダイレクト）
- [x] ロールガード（`/admin/hospitals`・`/admin/reservations`はROLE_HOSPITAL_ADMIN/ROLE_SYSTEM_ADMIN、`/admin/users`はROLE_SYSTEM_ADMIN限定）
- [x] ページリロード時のアクセストークン復元（`App.tsx`起動時に`refreshToken`があれば`/auth/refresh`を呼びaccessTokenを復元。復元完了までスピナー表示）
  - refreshTokenはローテーション式（使用後失効）のため、React 18 StrictModeのeffect二重実行対策として`useRef`で初回のみ実行するガードを追加

---

## 🔖 設計未決定事項（TODO）

- [x] **品種（breed）フィールドの管理方針** — 自由入力文字列で確定（2026-07-05、`feat/spring/pet-fields`）。マスタテーブルは導入せず、統計精度が必要になれば再検討

- [ ] **画像アップロード方針**
  - ペット写真・病院写真の保存先（S3 / ローカルストレージ）
  - Spring の `MultipartFile` 受け口 vs 別途ファイルサーバー

- [ ] **ソーシャルログイン（Google / LINE）**
  - OAuth2 フロー実装（Spring Security OAuth2 Client）
  - Frontend のコールバックハンドリング

---

## 🔮 将来拡張候補

- [ ] CI/CD（GitHub Actions — build / test / deploy）
- [ ] Django Analysis API との Frontend 連動（グラフ表示）
- [ ] プッシュ通知（FCM）
- [ ] 多言語対応（i18n — 日本語 / 韓国語）
- [ ] ダークモード切替 UI
- [ ] Nginx リバースプロキシ設定
- [ ] AWS / GCP デプロイ
