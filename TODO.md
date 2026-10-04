# Daipetto — 作業TODOリスト

最終更新: 2026-10-04

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

### Notification API（2026-10-03実装、Claude Code）
- [x] `Notification` ドメイン・エンティティ・テーブル作成（`V10__create_notifications.sql`）
  - `vaccination_id`は列のみ作成しFKは保留（`vaccinations`未作成。`docs/06_ERD` §15注記）
- [x] `GET /api/v1/notifications` — 通知一覧（作成日時の降順、本人宛のみ）
- [x] `PATCH /api/v1/notifications/{id}/read` — 既読（未読のみを対象にした条件付きUPDATEで冪等）
- [x] 予約承認・却下・診療完了時の通知生成（`NotifyReservationEventUseCase`、`08_State_Design` §6-6）
  - 状態遷移と同一トランザクション。遷移が成立しなかった場合は通知を作らない
  - キャンセルは §6-6 に通知定義が無いため生成しない
- [x] `NOTIFICATION-001`（存在しない通知）・`NOTIFICATION-002`（受信者本人でない）追加
- [ ] 予防接種リマインダー Scheduler（`VACCINATION`種別。Vaccination ドメイン実装後）

### Health Record API（2026-10-03実装、Claude Code）
- [x] `HealthRecord` ドメイン・エンティティ・テーブル作成（`V9__create_health_records.sql`）
- [x] `POST /api/v1/pets/{petId}/health-records` — 登録（`recordedDate`省略時は当日）
- [x] `GET /api/v1/pets/{petId}/health-records` — 一覧（記録日の降順、論理削除分を除く）
- [x] `PATCH /api/v1/health-records/{id}` — 更新（docs 6-3のPUTはプロジェクト規約に合わせPATCHへ変更）
- [x] `DELETE /api/v1/health-records/{id}` — 論理削除
- [x] `HEALTH-001`（存在しない健康記録）・`HEALTH-002`（存在しない/他ユーザーのペット）追加
  - 健康記録は`user_id`を持たないため所有者判定はペット経由

> **この回は練習用ギャップを作っていない**（ユーザーの指示: 「이번엔 따로 안남겨도 돼」）。新ドメインに1〜2機能を残す方針（`AGENTS.md` §11-1）は次回以降も有効。

**Codexレビュー（REVIEW-001、2026-10-03）への対応 — Claude Code**
- [x] R-01 [P1] 健康記録のPATCHで未指定項目が消える → `HealthRecord.patch`で未指定（null）は既存値を維持、`symptom`/`memo`は空文字で消去
- [x] R-02 [P2] PATCHが同時に論理削除された記録を復活させる → 内容列だけを`deleted_at IS NULL`の条件付きUPDATEで書き、0件ならHEALTH-001
- [x] R-03 [P2・既存] 病院情報の更新が停止を取り消す → 情報更新（name/address/phone）と停止（status）を別UPDATEに分離（逆方向の上書きも防止）
- [x] 同じ「読み取ったエンティティ全体を保存」パターンをペット更新（論理削除の復活）と営業時間更新（削除行の作り直し）でも修正。予約枠のBLOCKED切替はstatus1列のみの操作のため対象外
- [x] D-01 文書の実装状況表記（ERDの`未作成`、AGENTS.md §6）を修正。あわせて`docs/11` HOSP-T002の期待値をRESERVATION-002→009に修正（2026-09-13のR-05対応時の同期漏れ）
- 検証: `./gradlew clean test` 208件green。実PostgreSQLで決定的インターリーブ（SQLが競合更新を保持→HTTPがUPDATEで待機→commit）による受け入れ確認10件全通過。Codexの再現スクリプト（無改変）はR-01の欠陥アサーションで停止＝欠陥が再現しないことを確認

**Codex再レビュー（REVIEW-002、2026-10-04）への対応 — Claude Code**
- [x] R-04 [P2] 同時実行の部分PATCHが未指定項目を古い値で戻す → 健康記録の更新は行ロック付きで読み取り（`findByIdForUpdate`）、別PATCH・論理削除と直列化
- [x] T-01 [P3] スモークの記録日アサーションがUTC日付と比較していた → サーバー時刻（Asia/Tokyo）で比較し、日付境界をまたぐ場合も許容
- [x] 基準タイムゾーンをJST（Asia/Tokyo）に統一（ユーザー指示、日本向けサービスのため）: Spring APIは`main`でJVM既定を固定、`ServerTime`をAsia/SeoulからAsia/Tokyoへ、フロントの「今日」をUTC計算（`toISOString()`）からJSTへ（`docs/07` §2-7、`docs/12` §3-13）
- [ ] **要判断（既存の文書・実装の食い違い）**: `docs/07` §2-7 の例はオフセット付き（`2026-05-17T10:00:00+09:00`）だが、APIは`LocalDateTime`をオフセットなし（`2026-05-17T10:00:00`）で返している。オフセットを付けるか、文書をオフセットなし（JST）に直すかはユーザー判断
- 検証: `./gradlew clean test` 209件green。実PostgreSQLでR-04受け入れ3件（weightのみ・空のPATCH・別項目の2PATCH同時）、REVIEW-001受け入れ10件、スモーク14件（00:43 KST＝UTC日付が前日の時間帯）、通知チェック4件、認証/予約e2e 12件が全通過
- [ ] **要判断（既存の文書・実装の食い違い）**: `docs/07_API_Design` §5-4 はペット更新を「部分更新」としているが、実装は`birthDate`/`weight`省略時にNULLで上書きする全体置換。フロントは常にフォーム全体を送るため実害は出ていない。実装を部分更新に揃えるか、文書を全体置換に直すかはユーザー判断（今回は変更していない）

**検証（2026-10-03）**: テスト31件追加で`./gradlew clean test` 192件green。実PostgreSQL（捨てDB `daipetto_hn`）でV9/V10適用・`ddl-auto: validate`通過を確認し、API 14項目のスモークも全通過（`.ai-collab/tasks/2026-10-03-health-notification/scripts/smoke-health-notification.mjs`）

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

## Frontend — API連動の実装状況・残作業

### Hospital API 連動（2026-08-09 実装、Claude Code）
- [x] 病院検索 (`HospitalSearchPage`) — Hospital API 連動。distance/rating/reviews/specialtyはバックエンドに存在しないため削除
- [x] 病院詳細 (`HospitalDetailPage`) — Hospital + HospitalBusinessHours API 連動。doctors/reviewsはバックエンドに存在しないため削除、診療時間は曜日別に表示
- [x] 管理者画面 (`AdminHospitalPage`) — Hospital（Create/Update/Suspend）+ HospitalSchedule（Create/Block/Unblock）+ HospitalBusinessHours（Create/Update/Delete）全連動
  - `hospital_admins`未実装のため「自分の病院」を判定できず、一覧から選択する方式で暫定対応
- [x] `hospitalStore.ts` — 病院一覧・詳細・予約枠・営業時間（読み取り用）
- [x] `api/hospital.ts` — 全Hospital関連API（公開4 + 管理者6）のラッパー
- [x] プラットフォーム別APIベースURL — `Capacitor.getPlatform()`で実行時に自動判定（Web/Android/iOS）、`.env`の手動書き換えが不要に

### Reservation / HealthRecord / Notification API 連動（2026-10-04 実装、Claude Code、ブランチ `feat/react-api-integration`）
- [x] 予約 (`ReservationPage`) — 病院詳細・予約枠一覧から予約可能日（今日以降のAVAILABLE枠）と時間を選び`POST /reservations`。BLOCKED枠は選択不可。重複などのエラーはAPIのメッセージを表示
  - 診療目的はAPIに項目が無いため任意選択とし、`memo`の先頭に「診療目的: …」として付記（`docs/05` SC-010）
- [x] 予約履歴 (`ReservationHistoryPage`) — 一覧・詳細（メモ表示）・キャンセル（REQUESTED/APPROVEDのみ）。却下(REJECTED)も表示。ダミーの「病院詳細」（予約IDを病院IDとして使っていた）と「レビューを書く」（機能なし）は削除
- [x] 健康記録 (`HealthRecordPage`) — 一覧・登録・編集（PATCH）・削除。記録種別の選択（予防接種・投薬など）はモデルに無いため廃止し、体重・症状・メモの入力に変更
- [x] 通知 (`NotificationsPage`) — 一覧・既読化（一括既読APIが無いため「すべて既読」は1件ずつPATCH）。日時はJSTのまま表示
- [x] ダッシュボード — 次の予約（今日以降のREQUESTED/APPROVEDで最も早いもの）と、先頭ペットの健康記録（直近の体重・前回比・最近の症状）を実データ化
- [x] ログアウト — ヘッダー右上（存在しない`/profile`へのリンクだった）をログアウトボタンに変更。`POST /auth/logout`の後、失敗してもローカルのトークンを破棄
- [x] ペット一覧の表示修正 — 一覧API（PetSummary）は品種・性別を返さないため、常に「— · メス」と表示されていた。種別と体重の表示に変更
- [ ] 管理者画面 `AdminReservationPage` — **要判断**: 承認/却下/完了APIはあるが、管理者が予約を一覧するAPIが無い（`docs/05` SC-013は一覧表示を定義、`docs/07`に該当APIなし）。Spring側に管理者用一覧APIを追加するか、ユーザー判断待ち
- [ ] 管理者画面 `AdminUserPage` — ユーザー管理API連動（バックエンド未実装のため連動不可）

### 状態管理追加
- [ ] `reservationStore.ts` / `notificationStore.ts` — 未作成。各画面がAPIを直接呼ぶ形で足りているため。未読数バッジなど複数画面で状態を共有する時に追加する

### Codexレビュー後の修正・検証（2026-10-04）

> 上記の `[x]` はAPI接続の実装済みを示し、レビュー承認を意味しない。判定は **CHANGES_REQUESTED**。詳細: `.ai-collab/tasks/2026-10-04-frontend-api-integration/REVIEW-001.md`。
>
> 再レビュー（`REVIEW-002.md`、2026-10-04）でF-01〜F-03は **ACCEPTED**。ブラウザ回帰とAndroid APKビルドはClaude Codeの報告のみで、Codexは未実施。

- [x] F-01 [P1] アカウント切替時に前ユーザーのペット一覧・選択状態を破棄し、前セッションの遅延レスポンスによる再保存も防止する。A→ログアウト→B＋取得失敗/応答遅延で他ユーザーの情報が表示されないことを確認
  - 修正（Claude Code、HANDOFF-002 → Codex REVIEW-002でACCEPTED）: `authStore.session`（clearAuthごとに増加）で`petStore`を破棄し、古いセッションで始まった応答は反映しない。ログアウトせずに別アカウントでログインした場合も`LoginPage`がclearAuthして新セッションにする。マイペットは取得失敗を「未登録」と区別して再試行を表示
- [x] F-02 [P2] 病院詳細・予約枠をhospitalId単位で管理し、古い病院の応答を無視する。切替時は選択をリセットし、現在の病院の取得完了まで進行不可にする。取得エラーと再試行も表示
  - 修正（同上）: `ReservationPage`は病院IDをkeyに作り直し、病院情報・予約枠をその病院専用のローカル状態で取得（遅れて届いた応答は破棄）。取得完了まで「次へ」不可、失敗時は再試行を表示。共有の`hospitalStore`も最後に要求した病院の応答だけを反映し、別の病院へ切り替えた時点で前のデータを消す（病院詳細・管理者画面にも効く）
- [x] F-03 [P2] 予約枠とダッシュボードの「次の予約」をJSTの開始日時で判定する。同日でも過去時刻を除外し、確認・送信時にも再確認する
  - 修正（同上）: `utils/date.ts`の`isUpcoming`（開始がJSTの現在より後。バックエンドのRESERVATION-005と同じ判定）を予約可能日・時間枠・送信前の再確認・ダッシュボードに適用。ダッシュボードは取得失敗を空データと区別して表示
- [x] Codex実行: `npm run build`（TypeScript/Vite）成功。実ストアを使うNode再現でF-01/F-02を確認（欠陥再現であり受け入れ成功ではない）
- [x] 修正後のブラウザ回帰確認: アカウント切替、応答順序逆転、ネットワーク失敗、JST当日の過去/未来枠、通常の予約・健康記録・通知・ログアウト
  - Claude Code実施（2026-10-04 21時台JST、捨てDB）。結果は`HANDOFF-002.md`。Node再現（実ストア）`scripts/fix-acceptance.cjs` 10件も追加。Codexの確認は未実施
- [ ] Androidエミュレーター/実機でAPI疎通・認証・主要操作を確認（Claudeによるdebug APKビルド成功の報告あり、端末実行は未検証）
- [ ] iOSビルド・実行確認（syncのみ実施との報告。Windowsではビルド未実施）
- [ ] ペット編集画面と`PetUpdateRequest`の`petType`必須項目を対応する（バックエンド更新APIは実装済み、現在の画面接続範囲外）

### バックエンドレビュー確定
- [x] HealthRecord/NotificationのR-01〜R-04・T-01・D-01: Codex `2026-10-03-health-notification/REVIEW-003.md`でACCEPTED（2026-10-04）。209テスト、実PostgreSQLの部分更新/削除/停止/通知/認証予約の検証済み。今回のFrontend承認とは別

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
