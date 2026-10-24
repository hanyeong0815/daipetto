# 単体テスト（だいペット / Daipetto）

全機能の単体テストの計画・実施方法・結果・エビデンスをまとめたフォルダ。

- 実施日: 2026-10-23
- 対象ブランチ: `test/unit-test`（`develop` の全機能を含む）
- 実施体制:
  - テスト設計（テスト項目の作成）: Claude Code（Opus 5.5）
  - テスト実行・エビデンス取得: Claude Code。画面単体テストの一部は Sonnet 5.5 のサブエージェントが実行
  - テスト設計のレビュー・結果の独立検証: Codex（`gpt-6.1-sol`）。記録は [reviews/](reviews/)

## 1. 結果の要約

結果の一覧は [RESULTS.md](RESULTS.md)、見つかった不具合と対応は [DEFECTS.md](DEFECTS.md)。

| 種類 | 件数・結果 |
| --- | --- |
| API単体テスト（実HTTP・実DB） | **134/134 OK**（全52エンドポイント、機能別は RESULTS.md） |
| 自動テスト（JUnit） | **342/342 合格** |
| 画面単体テスト | **33/33 OK**（[spec/UT-14_screens.md](spec/UT-14_screens.md)） |
| カバレッジ（JaCoCo） | 行（C0相当）86.9%、分岐（C1相当）76.7%、命令 88.7% |
| 不具合・指摘 | 実装6件（D-003〜005・007〜009）、設計書3件（D-001・002・006）を修正し、再テストで解消（DEFECTS.md） |
| テスト設計レビュー（Codex） | 未テストの分岐・エンドポイント11件（G-01〜G-11）の指摘を項目に追加（DEFECTS.md §3） |

## 2. 対象範囲

| 区分 | 対象 |
| --- | --- |
| Spring API | 全52エンドポイント（認証、ユーザー、ペット、健康記録、ワクチン接種予定、病院、病院管理者、診療時間、予約枠、予約、予約管理、通知、ユーザー管理） |
| 定期処理 | ワクチン接種リマインダー（毎日9時）、予約枠の自動生成（毎月1日）。いずれも処理本体を JUnit で確認（cronの起動そのものは対象外） |
| 共通処理 | 認証フィルター（JWT）、共通エラー形式、入力チェック、日時の形式（+09:00） |
| 画面 | SC-001〜SC-016（ログイン、会員登録、ダッシュボード、ペット、健康記録、ワクチン、病院、予約、通知、管理画面） |
| 対象外 | 分析API（FastAPI。単体テスト後の 2026-10-25 に追加。検証は `.ai-collab/tasks/2026-10-25-analysis-api/`）、ネイティブアプリ（Capacitor）、性能・負荷 |

## 3. テストの種類と判定基準

| 種類 | 内容 | 判定基準 | エビデンス |
| --- | --- | --- | --- |
| 自動テスト（JUnit） | Service・Controller・Persistence・ドメインのクラス単位のテスト。Repository はモック、Persistence は H2、Controller は `@WebMvcTest` | 全テストが合格 | `evidence/junit/*.xml`、`evidence/coverage/`（JaCoCo） |
| API単体テスト | 使い捨ての PostgreSQL で起動した Spring API に実際にリクエストを送り、レスポンスとDBの状態を確認。期待結果は API設計書（docs/07）の正常系・異常系 | 全確認項目（HTTPステータス・エラーコード・応答内容・DBの状態）が期待結果と一致 | `evidence/api/<項目ID>.json`（リクエスト・レスポンス・DB確認・判定） |
| 画面単体テスト | ブラウザで各画面を表示・操作し、表示内容と操作結果を確認 | 期待した表示・遷移・DB反映 | `evidence/screen/*.jpg`（画面写し）・`*.json`／`*.txt`（画面に写らない確認） |

エビデンスにはパスワードを残さない（伏せ字）。Token は先頭12文字以外を伏せる。

## 4. 実施環境

| 項目 | 内容 |
| --- | --- |
| OS | Windows 11 |
| Java / Spring Boot | Java 17 / Spring Boot 3.3.13（`spring-api`） |
| Node / Vite | Node 24.17.0 / Vite 6.4（`frontend`） |
| DB | PostgreSQL 16.9（Dockerコンテナ `daipetto-postgres`）。テストごとに使い捨てのDB（API: `daipetto_ut`、画面: `daipetto_ut_screen`）を作成し、終了後に削除 |
| サーバー | テスト専用に 18081（API単体テスト）・18082（画面単体テスト）で起動。普段のDB `daipetto`・ポート8080は使わない |
| ブラウザ | Claude Code デスクトップアプリの内蔵ブラウザ（Chromium） |

## 5. 実施手順（再現方法）

必要なもの: JDK 17、Node 24、Docker（`docker compose up -d` で `daipetto-postgres` を起動済み）。

事前準備（初回のみ）: `spring-api/.env.example` を `spring-api/.env` にコピーし、`DB_USERNAME`・`DB_PASSWORD` をリポジトリ直下の `.env` の `POSTGRES_USER`・`POSTGRES_PASSWORD` と同じ値に、`JWT_SECRET` を32文字以上の任意の文字列にする。Spring APIの `local` プロファイルは、起動したフォルダの `.env` を読み込むため、Spring APIは必ず `spring-api/` で起動する。`POSTGRES_USER` が `daipetto_user` でない場合は、以下の `-U daipetto_user` をその値に読み替え、テスト実行のコマンドに `PGUSER=<その値>` を付ける（スクリプトがDBの状態を確認するときに使う）。

コマンドはリポジトリ直下（`daipetto/`）で実行する。ただし手順2の Spring API の起動は、別のターミナルを開いて `spring-api/` で実行し、API単体テストが終わるまで起動したままにする（終わったら Ctrl+C で停止）。

```bash
# 1. 自動テスト（JUnit）とカバレッジ
cd spring-api && ./gradlew test && cd ..

# 2. API単体テスト（使い捨てDB）。再実行するときは必ずDBを作り直す（前回のデータが残ると件数を確認する項目が合わない）
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE IF EXISTS daipetto_ut;"
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_ut;"
cd spring-api && ./gradlew bootJar && cd ..

#    別のターミナル（spring-api/ で起動。DB名とポートだけをテスト用に上書きする）
cd spring-api
java -jar build/libs/spring-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --DB_NAME=daipetto_ut --server.port=18081

#    元のターミナル（リポジトリ直下）。起動ログに「Started SpringApiApplication」が出てから実行する
BASE=http://localhost:18081 DB=daipetto_ut node test/scripts/run-api-unit-tests.mjs

# 3. 仕様書・結果の生成（JUnit結果・JaCoCoを evidence にコピーし、spec/*.md と RESULTS.md を作る）
node test/scripts/build-spec.mjs

# 4. 後片付け
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_ut;"
```

画面単体テストは `test/scripts/seed-screen-data.mjs` でデータを用意し、ブラウザで [spec/UT-14_screens.md](spec/UT-14_screens.md) の手順を実施した。

## 6. フォルダ構成

```
test/
├── README.md            … このファイル（計画・環境・手順）
├── RESULTS.md           … 結果サマリ（機能別件数・カバレッジ）※自動生成
├── DEFECTS.md           … 不具合・指摘管理表
├── spec/                … 機能別の単体テスト仕様書・結果（UT-01〜UT-13は自動生成、UT-14は画面）
├── evidence/
│   ├── api/             … API単体テストのエビデンス（項目ごとのJSON、_summary.json）
│   ├── junit/           … JUnitの結果XML
│   ├── coverage/        … JaCoCoのカバレッジレポート（XML・HTML）
│   └── screen/          … 画面単体テストの画面写し・確認結果
├── reviews/             … Codexのレビュー記録（写し）
└── scripts/             … テストの実行・データ準備・仕様書生成のスクリプト
```
