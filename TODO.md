# Daipetto — 作業TODOリスト

最終更新: 2026-06-27

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

### Hospital API
- [ ] `Hospital` ドメイン・エンティティ・テーブル作成 (`V4__create_hospitals.sql`)
- [ ] `POST /api/v1/hospitals` — 病院登録（HOSPITAL_ADMIN）
- [ ] `GET /api/v1/hospitals` — 病院一覧（検索・フィルタ対応）
- [ ] `GET /api/v1/hospitals/{id}` — 病院詳細
- [ ] `PATCH /api/v1/hospitals/{id}` — 病院情報更新
- [ ] Hospital 関連テスト

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

### 実 API 接続が必要な画面
- [ ] 病院検索 (`HospitalSearchPage`) — Hospital API 連動
- [ ] 病院詳細 (`HospitalDetailPage`) — Hospital API 連動
- [ ] 予約 (`ReservationPage`) — Reservation API 連動
- [ ] 予約履歴 (`ReservationHistoryPage`) — Reservation API 連動
- [ ] 健康記録 (`HealthRecordPage`) — HealthRecord API 連動
- [ ] 通知 (`NotificationsPage`) — Notification API 連動
- [ ] 管理者画面 (`AdminReservationPage` / `AdminHospitalPage` / `AdminUserPage`) — 各 API 連動

### 状態管理追加
- [ ] `reservationStore.ts` — 予約一覧・詳細
- [ ] `hospitalStore.ts` — 病院一覧・詳細
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
