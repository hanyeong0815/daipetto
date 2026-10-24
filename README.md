# Daipetto（だいペット）— ペット健康管理・動物病院予約

ペットの健康記録・ワクチン接種予定・動物病院の予約・体重推移などの分析をまとめて管理するアプリです。
Spring Boot（業務API）+ FastAPI（分析API）+ React（Web / Capacitor でモバイル）のポートフォリオプロジェクトです。
設計書（`docs/`、日本語13文書）と実装の整合を保ちながら開発しています。

---

## 技術スタック

| 領域 | 技術 |
|---|---|
| Frontend | React 18 / TypeScript 5.7 / Vite 6.4 / Tailwind CSS 3.4 / Zustand 5 / Axios 1.18 / React Router 6.30 |
| モバイル | Capacitor 8.4（Android / iOS） |
| 業務API | Java 17 / Spring Boot 3.3.13 / Spring Security / Spring Data JPA / Flyway / MapStruct |
| 分析API | Python 3.12.10 / FastAPI 0.162 / uvicorn / psycopg 3（Spring からのみ呼ばれる内部API） |
| 認証 | JWT（Access Token 30分 + Refresh Token 14日、使うたびに再発行） |
| DB | PostgreSQL 16.9 |
| インフラ | Docker Compose |
| テスト | JUnit 5 / Mockito / JaCoCo、pytest |

タイムゾーンはすべて JST（Asia/Tokyo）です。

---

## システム構成

```
ブラウザ・モバイルアプリ（React）
        │  REST / JSON（JWT）
        ▼
業務API（Spring Boot）── 認証・認可・ペットの所有者チェック
        │  内部API（X-Internal-Api-Key）
        ▼
分析API（FastAPI）── 体重推移・健康スコア・次回接種推奨日（読み取りのみ）
        │
        ▼
PostgreSQL（テーブルは Spring の Flyway が管理）
```

フロントエンドは Spring だけを呼びます。分析API はブラウザから直接呼ばれません（`docs/04` §8、`docs/09` §4-6）。

---

## アーキテクチャ

**業務API（Spring）** はヘキサゴナル構成です。

```
Controller
  └─ UseCase Interface
       └─ UseCase Service（ビジネスロジック）
            └─ Domain Model（状態遷移はここ）
                 └─ Repository Port / Client Port
                      └─ Persistence Adapter（MapStruct Mapper ↔ JPA Entity ↔ PostgreSQL）
                         Client Adapter（分析APIの呼び出し）
```

- Domain は JPA に依存しません。エラーは `ErrorCode + CustomException + Preconditions.validate` で統一しています
- 重要な更新は対象の列だけを条件付きUPDATEで書き、読み取ったエンティティ全体を保存し直しません

**分析API（FastAPI）** は Router → Schema（Pydantic）→ Service（DBに依存しない計算）→ Repository（SQL）の構成です。

---

## 画面構成

| 画面 | パス |
|---|---|
| ログイン / 会員登録 | `/login` `/register` |
| ダッシュボード | `/dashboard` |
| マイペット一覧 / 登録 / 詳細 / 編集 | `/pets` `/pets/new` `/pets/:petId` `/pets/:petId/edit` |
| 健康記録 | `/pets/:petId/health` |
| ワクチン接種予定 | `/pets/:petId/vaccinations` |
| 病院検索 / 詳細 / 予約（3ステップ） | `/hospitals` `/hospitals/:hospitalId` `/hospitals/:hospitalId/reserve` |
| 予約履歴 | `/reservations` |
| 通知 | `/notifications` |
| 管理画面（予約・病院／病院管理者、ユーザー／システム管理者） | `/admin/reservations` `/admin/hospitals` `/admin/users` |

---

## ローカル起動

### Docker Compose

```bash
cp .env.example .env    # DB・JWT・内部APIキー（INTERNAL_API_KEY）を設定
docker compose up
```

| サービス | URL |
|---|---|
| Frontend | http://localhost:5173 |
| 業務API（Spring） | http://localhost:8080 |
| 分析API（FastAPI、内部API） | http://localhost:8000 |
| PostgreSQL | localhost:5432 |

### 個別起動（開発時）

**業務API（Spring、IntelliJ）**

```bash
cd spring-api
cp .env.example .env    # DB・JWT・ANALYSIS_API_URL・INTERNAL_API_KEY
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local` プロファイルは起動したフォルダの `.env` を読み込みます（`spring-api/` で起動してください）。

**分析API（FastAPI）**

```bash
cd analysis-api
uv venv --python 3.12.10 .venv
uv pip install --python .venv -r requirements-dev.txt
cp .env.example .env    # DB と INTERNAL_API_KEY（Spring と同じ値）
.venv/Scripts/python -m uvicorn app.main:app --port 8000   # macOS/Linux は .venv/bin/python
```

**Frontend**

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

---

## 環境変数

| ファイル | 用途 |
|---|---|
| `.env`（ルート） | Docker Compose が読む変数（DB・JWT・内部APIキー） |
| `spring-api/.env` | Spring がローカル実行時に読む変数（DB・JWT・分析APIのURLとキー） |
| `analysis-api/.env` | 分析APIがローカル実行時に読む変数（DB・内部APIキー） |
| `frontend/.env.local` | Vite が読む変数（実行環境別のAPIのURL） |

実ファイルは `.gitignore` で除外しています。各ディレクトリの `.env.example` をコピーして使ってください。
内部APIキー（`INTERNAL_API_KEY`）は Spring と分析APIで同じ値にします。

---

## 主なAPI

全API（56本、うち練習用の未実装1本）の仕様は `docs/07_API_Design*.md` にあります。

```
POST   /api/v1/users                                 会員登録
POST   /api/v1/auth/login | refresh | logout         ログイン・トークン再発行・ログアウト
GET    /api/v1/users/me                              自分のプロフィール

GET    /api/v1/pets  POST /api/v1/pets               ペット一覧・登録
GET | PATCH | DELETE /api/v1/pets/{petId}            ペット詳細・更新・削除
GET | POST /api/v1/pets/{petId}/health-records       健康記録（更新・削除は /health-records/{id}）
GET | POST /api/v1/pets/{petId}/vaccinations         ワクチン接種予定（完了・中止・再登録・削除は /vaccinations/{id}/...）

GET    /api/v1/hospitals                             病院検索（keyword・area）
GET    /api/v1/hospitals/{hospitalId}[/schedules | /business-hours]

POST   /api/v1/reservations  GET /api/v1/reservations[/{id}]   予約申請・一覧・詳細
PATCH  /api/v1/reservations/{id}/cancel              予約キャンセル

GET    /api/v1/notifications  PATCH .../{id}/read    通知一覧・既読

GET    /api/v1/analysis/pets/{petId}/weight          体重推移（期間指定）
GET    /api/v1/analysis/pets/{petId}/health-score    健康スコア（直近30日）
GET    /api/v1/analysis/pets/{petId}/vaccination/next  次回接種推奨日

/api/v1/admin/...   病院登録・更新・停止、営業時間、予約枠（自動生成を含む）、
                    予約の承認・却下・診療完了・一覧、病院管理者の紐付け、ユーザー管理
```

レスポンス形式は全エンドポイント共通です。

```json
{ "success": true, "data": {} }
{ "success": false, "data": null, "code": "PET-001", "message": "存在しないペットです。" }
```

---

## テスト

```bash
cd spring-api && ./gradlew clean test       # JUnit（JaCoCo のレポートも生成）
cd analysis-api && .venv/Scripts/python -m pytest
```

全機能の単体テスト（テスト項目・実行結果・エビデンス、API単体テスト・画面単体テスト・カバレッジ）は [test/README.md](test/README.md) にまとめています。

---

## モバイルアプリ（Capacitor）

React のビルドを Android / iOS の WebView に載せる構成です。ネイティブ SDK（Android Studio / Xcode）が別途必要です。

```bash
cd frontend
npm run cap:android   # ビルド → cap sync → Android Studio が開く → Run
npm run cap:ios       # ビルド → cap sync → Xcode が開く → Run（Mac のみ）
npm run cap:sync      # コード変更後の反映（ビルド + 全プラットフォームへ同期）
```

APIのURLは実行環境（Web / Android / iOS）を Capacitor が判定して自動で切り替えます（`frontend/.env.example` の `VITE_API_BASE_URL_WEB` / `_ANDROID` / `_IOS`）。Android エミュレーターからホストPCへは `10.0.2.2` で接続します。

開発中のライブリロードは `capacitor.config.ts` の `server.url` を Vite dev server（例: `http://10.0.2.2:5173`）に設定します。**本番ビルドの前に `server` ブロックを削除**してください。

---

## 実装状況

詳細は [TODO.md](./TODO.md) を参照。

**完了**
- 認証（会員登録・ログイン・JWT・Refresh Token の再発行・ログアウト）、利用停止・権限変更の即時反映
- ペット CRUD（品種・不妊手術・マイクロチップ）、健康記録 CRUD、ワクチン接種予定（完了・中止・再登録・毎日9時のリマインダー通知）
- 病院（登録・更新・停止・検索）、営業時間、予約枠（手動登録・ブロック・営業時間からの月次自動生成）、病院管理者の紐付け
- 予約（申請・キャンセル・承認・却下・診療完了、同時実行の対策）と通知
- ユーザー管理（検索・停止・停止解除・権限変更）
- 分析API（体重推移・健康スコア・次回接種推奨日）と Spring の中継
- フロントエンド全画面の実API連動、ロール別の画面ガード
- 全機能の単体テスト（`test/`）

**残り**
- 症状統計API（練習用にあえて未実装）
- 分析結果のグラフ表示（フロントエンド）
- Android / iOS 実機での動作確認
- 画像アップロード・ソーシャルログイン（方針未決定）、CI/CD
