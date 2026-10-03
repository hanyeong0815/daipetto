# Daipetto — ペット健康管理アプリ

大切なペットの健康記録・病院予約・体重推移を一元管理するWebアプリです。  
Spring Boot + React のポートフォリオプロジェクトとして開発中。

---

## 技術スタック

| 領域 | 技術 |
|---|---|
| Frontend | React 18 / TypeScript / Vite / Tailwind CSS |
| 状態管理 | Zustand 5 |
| HTTP | Axios 1.18（JWT自動付与・401リフレッシュ対応） |
| Backend | Spring Boot 3 / Java 17 / Spring Security |
| 認証 | JWT（アクセストークン + リフレッシュトークン） |
| DB | PostgreSQL 16 / Flyway |
| Analysis | Django REST Framework（実装予定） |
| インフラ | Docker Compose |

---

## アーキテクチャ

Spring API はクリーンアーキテクチャで構成しています。

```
Controller
  └─ UseCase Interface
       └─ UseCase Service（ビジネスロジック）
            └─ Repository Port
                 └─ Persistence Adapter
                      └─ MapStruct Mapper ↔ JPA Entity ↔ PostgreSQL
```

Domain と JPA Entity を分離し、ドメイン層が永続化の詳細に依存しない設計にしています。  
エラー処理は `ErrorCode + CustomException + Preconditions.validate` パターンで統一。

---

## 画面構成

| 画面 | パス |
|---|---|
| ログイン / 会員登録 | `/login` `/register` |
| ダッシュボード | `/dashboard` |
| マイペット一覧 / 登録 / 詳細 | `/pets` |
| 健康記録 | `/pets/:id/health` |
| 病院検索 / 詳細 | `/hospitals` |
| 予約（3ステップ） / 履歴 | `/hospitals/:id/reserve` |
| 通知 | `/notifications` |
| 管理者画面（病院・予約・ユーザー） | `/admin/*` |

---

## ローカル起動

### Docker Compose（推奨）

```bash
# 1. 環境変数ファイルを作成
cp .env.example .env
# .env を編集してパスワードなどを設定

# 2. 起動
docker compose up
```

| サービス | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Spring API | http://localhost:8080 |
| Django API | http://localhost:8000 |
| PostgreSQL | localhost:5432 |

### 個別起動（開発時）

**Spring API（IntelliJ）**

```bash
cd spring-api
cp .env.example .env
# .env を編集
```

IntelliJ の Run Configuration で `local` プロファイルを指定して実行。  
`spring.config.import=optional:file:.env[.properties]` で `.env` を自動読み込みします。

**Frontend**

```bash
cd frontend
cp .env.example .env.local
# VITE_API_BASE_URL を確認
npm install
npm run dev
```

---

## 環境変数

| ファイル | 用途 |
|---|---|
| `.env`（ルート） | Docker Compose が読む変数（DB・JWT・内部APIキー） |
| `spring-api/.env` | Spring Boot がローカル実行時に読む変数 |
| `frontend/.env.local` | Vite が読む変数（APIのベースURL） |

実ファイルは `.gitignore` で除外済み。各ディレクトリの `.env.example` をコピーして使ってください。

---

## 主なAPI

```
POST   /api/v1/auth/register     # 会員登録
POST   /api/v1/auth/login        # ログイン
POST   /api/v1/auth/refresh      # トークン再発行
POST   /api/v1/auth/logout       # ログアウト

GET    /api/v1/users/me          # 自分のプロフィール

POST   /api/v1/pets              # ペット登録
GET    /api/v1/pets              # ペット一覧
GET    /api/v1/pets/:id          # ペット詳細
PATCH  /api/v1/pets/:id          # ペット情報更新
DELETE /api/v1/pets/:id          # ペット削除

GET    /api/v1/hospitals                          # 病院検索（keyword・area）
GET    /api/v1/hospitals/:id                      # 病院詳細
GET    /api/v1/hospitals/:id/schedules            # 予約枠一覧
GET    /api/v1/hospitals/:id/business-hours       # 営業時間一覧
POST   /api/v1/admin/hospitals                    # 病院登録（管理者）
# ほか管理者API: 病院更新/停止・予約枠登録/block/unblock・営業時間CRUD

POST   /api/v1/reservations                       # 予約申請
GET    /api/v1/reservations                       # 予約一覧
GET    /api/v1/reservations/:id                   # 予約詳細
PATCH  /api/v1/reservations/:id/cancel            # 予約キャンセル
PATCH  /api/v1/admin/reservations/:id/approve     # 予約承認（管理者）
PATCH  /api/v1/admin/reservations/:id/complete    # 診療完了（管理者）
```

全API仕様は `docs/07_API_Design*.md` を参照。

レスポンス形式は全エンドポイント共通：

```json
{ "success": true, "data": {} }
{ "success": false, "data": null, "code": "PET-001", "message": "ペットが見つかりません。" }
```

---

## モバイルアプリ（Capacitor）

React ビルドを Android / iOS の WebView に載せる構成です。  
ネイティブ SDK のインストール（Android Studio / Xcode）が別途必要です。

### 開発ワークフロー

```bash
cd frontend

# Android（Android Studio が必要）
npm run cap:android
# → ビルド → cap sync → Android Studio が開く → Run

# iOS（Xcode が必要 / Mac のみ）
npm run cap:ios
# → ビルド → cap sync → Xcode が開く → Run
```

### コード変更後の反映

```bash
npm run cap:sync   # ビルド + 全プラットフォームへ同期
```

### Android エミュレーターからローカル API を叩く場合

Android エミュレーターの `localhost` はホストマシンではなくエミュレーター自身を指します。  
`.env.local` の `VITE_API_BASE_URL` を `http://10.0.2.2:8080` に切り替えてください。

### 開発時のライブリロード

`capacitor.config.ts` の `server.url` を Vite dev server のアドレスに設定すると、  
ネイティブアプリ上でもブラウザと同様にホットリロードが効きます。

```
# Android エミュレーター → ホストマシンの Vite dev server
url: 'http://10.0.2.2:5173'
```

**本番ビルド時は `server` ブロックを削除**してから `npm run cap:android` / `npm run cap:ios` を実行してください。

---

## 実装状況

詳細は [TODO.md](./TODO.md) を参照。

**完了**
- 会員登録 / ログイン / JWT認証 / リフレッシュ（rotation） / ログアウト
- ペット CRUD API
- 病院 API（登録・更新・停止・検索・詳細・予約枠・曜日別営業時間）
- 予約 API（申請・一覧・詳細・キャンセル・承認・診療完了。却下のみ未実装）
- Frontend 全画面（認証・ペット・病院は実 API 連動、他はスタブ）
- Protected Route / ロールガード / リロード時トークン復元

**実装中・予定**
- 予約却下 API / 通知 API / 健康記録 API / ワクチン API
- 予約枠自動生成 Scheduler（曜日別営業時間から生成）
- Django Analysis API（体重推移・健康スコア）
