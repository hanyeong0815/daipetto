# Daipetto — 作業TODOリスト

最終更新: 2026-08-09

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

### Pet テーブル不足カラム追加
> フロントは送信済みだが DB・Entity に存在しないため現在は無視されている

- [ ] `V4__alter_pets_add_columns.sql` — `breed`, `neutered`, `microchip_number` カラム追加
  ```sql
  ALTER TABLE pets
    ADD COLUMN breed            VARCHAR(100) NULL,
    ADD COLUMN neutered         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN microchip_number VARCHAR(15)  NULL;
  ```
- [ ] `PetEntity` に `breed` / `neutered` / `microchipNumber` フィールド追加
- [ ] `Pet` ドメインモデルに同フィールド追加
- [ ] `PetMapper` の変換ロジック更新
- [ ] 既存テストが通ることを確認（`PetServiceTest`）

> Hospital API 追加後は V5 以降にずれるため、マイグレーションファイル番号に注意

### Hospital API（branch: `feat/spring/hospital`）
- [x] `Hospital` / `HospitalSchedule` ドメイン・エンティティ・テーブル作成
  - 実際のマイグレーション番号: `V5__create_hospitals.sql` / `V6__create_hospital_schedules.sql`（Pet列追加がV4を使用したためズレ）
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

### Reservation API
- [ ] `Reservation` ドメイン・エンティティ・テーブル作成 (`V5__create_reservations.sql`)
- [ ] 予約ステータス設計: `PENDING → CONFIRMED → COMPLETED / CANCELLED`
- [ ] `POST /api/v1/reservations` — 予約作成（USER）
- [ ] `GET /api/v1/reservations` — 予約一覧（ユーザー別）
- [ ] `GET /api/v1/reservations/{id}` — 予約詳細
- [ ] `PATCH /api/v1/reservations/{id}/confirm` — 予約承認（HOSPITAL_ADMIN）
- [ ] `PATCH /api/v1/reservations/{id}/cancel` — 予約キャンセル
- [ ] `PATCH /api/v1/reservations/{id}/complete` — 診療完了
- [ ] Reservation 関連テスト

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

### 認証フロー
- [ ] Protected Route 実装（未ログイン → `/login` にリダイレクト）
- [ ] ロールガード（HOSPITAL_ADMIN / SYSTEM_ADMIN ページ保護）
- [ ] ページリロード時のアクセストークン復元（`refreshToken` → `/auth/refresh` で再取得）

---

## 🔖 設計未決定事項（TODO）

- [ ] **品種（breed）フィールドの管理方針**
  - 案A: SYSTEM_ADMIN が品種マスタ管理 → ユーザーはドロップダウン選択
  - 案B: 自由入力 + オートコンプリート、リスト未登録品種はユーザーが追加申請可能
  - → 決定後、`GET /api/v1/breeds` API 実装要否も確定する

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
