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
```

レスポンス形式は全エンドポイント共通：

```json
{ "success": true, "data": {} }
{ "success": false, "data": null, "code": "PET-001", "message": "ペットが見つかりません。" }
```

---

## 実装状況

詳細は [TODO.md](./TODO.md) を参照。

**完了**
- 会員登録 / ログイン / JWT認証 / リフレッシュ / ログアウト
- ペット CRUD API
- Frontend 全画面（認証・ペット管理のみ実 API 連動、他はスタブ）

**実装中・予定**
- 病院 API / 予約 API / 通知 API / 健康記録 API
- Protected Route（未ログイン時のリダイレクト）
- Django Analysis API（体重推移・健康スコア）
