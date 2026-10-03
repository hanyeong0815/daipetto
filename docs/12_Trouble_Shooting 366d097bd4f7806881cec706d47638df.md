# 12_Trouble_Shooting

# **トラブルシューティング集**

## **ドキュメント情報**

| **項目** | **内容** |
| --- | --- |
| Project | 大ペット（だいペット） |
| Document | Trouble Shooting |
| Author | Koh Hanyeong |
| Status | Draft |
| Updated | 2026-10-03 |

---

# **1. ドキュメント概要**

本ドキュメントでは、「大ペット」開発中に発生する可能性がある代表的な問題、およびその原因・対応方法を整理する。

対象範囲:

- Frontend
- Spring Boot
- Django REST Framework
- PostgreSQL
- Docker Compose
- JWT認証
- Flyway Migration
- CORS
- Scheduler
- Build環境

---

# **2. Frontend関連**

# **2-1. npm install失敗**

## **症状**

```bash
npm install
```

実行時に dependency resolve error が発生する。

---

## **原因**

- Node.js Version不一致
- package-lock.json不整合
- 古いnode_modules残存

---

## **対応方法**

```bash
rm -rf node_modules
rm package-lock.json

npm install
```

---

## **確認ポイント**

```bash
node -v
npm -v
```

---

## **推奨Version**

| **項目** | **Version** |
| --- | --- |
| Node.js | 24.17.0 |
| npm | 11.13.0 |

---

# **2-2. Vite起動失敗**

## **症状**

```bash
npm run dev
```

実行時にPort Error発生。

---

## **原因**

5173 Port使用中。

---

## **対応方法**

使用中Port確認:

```bash
# Windows
netstat -ano | findstr 5173

# Mac
lsof -i :5173
```

Process終了:

```bash
#Windows
taskkill /f /pid PID

# Mac
kill -9 PID
```

---

# **2-3. API通信失敗（Frontend）**

## **症状**

Axios通信時にNetwork Error発生。

---

## **原因**

- Backend未起動
- CORS設定不備
- API URL誤設定

---

## **対応方法**

```
VITE_API_BASE_URL_WEB=http://localhost:8080
```

を確認（Android エミュレーターは `VITE_API_BASE_URL_ANDROID=http://10.0.2.2:8080`）。

---

# **2-4. Tailwind CSS適用されない**

## **原因**

- tailwind.config.js設定漏れ
- content path不一致

---

## **対応方法**

```jsx
content: [
  "./index.html",
  "./src/**/*.{js,ts,jsx,tsx}"
]
```

---

# **3. Spring Boot関連**

# **3-1. Gradle Build失敗**

## **症状**

```bash
./gradlew clean build
```

失敗。

---

## **原因**

- Java Version不一致
- Dependency Version競合
- springdoc互換性問題

---

## **対応方法**

```bash
java -version
```

確認。

---

## **推奨Version**

| **項目** | **Version** |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.3.13 |
| Gradle | 8.14.4 |

---

# **3-2. springdoc起動エラー**

## **症状**

```
NoClassDefFoundError
```

---

## **原因**

Spring Boot Versionとspringdoc Version不一致。

---

## **対応方法**

```groovy
// build.gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
```

利用。

---

# **3-3. ApplicationContext起動失敗**

## **症状**

```
Failed to load ApplicationContext
```

---

## **原因**

- Bean循環参照
- DB接続失敗
- Security Config誤設定

---

## **対応方法**

以下確認:

- application.yml
- JWT Secret
- DB接続情報
- SecurityConfig

---

# **3-4. H2 Test DBエラー**

## **症状**

```
DataSourceBeanCreationException
```

---

## **原因**

Test DB設定なし。

---

## **対応方法**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
```

---

## **Test Profile**

```java
// src/test/java/koh/portfolio/authservice/AuthServiceApplicationTests.java
@ActiveProfiles("test")
```

追加。

---

# **3-5. ActiveProfilesエラー**

## **症状**

```
シンボルを見つけられません
```

---

## **原因**

Import漏れ。

---

## **対応方法**

```java
// src/test/java/koh/portfolio/authservice/AuthServiceApplicationTests.java
import org.springframework.test.context.ActiveProfiles;
```

追加。

---

# **3-6. JWT認証失敗**

## **症状**

401 Unauthorized

---

## **原因**

- Token期限切れ
- Authorization Header不備
- Secret不一致

---

## **Header形式**

```
Authorization: Bearer access-token
```

---

## **JWT Secret確認**

```
JWT_SECRET=change-this-secret
```

---

# **3-7. CORS Error**

## **症状**

```
Blocked by CORS policy
```

---

## **原因**

許可Origin未設定。

---

## **対応方法**

```java
allowedOrigins(
  "http://localhost:5173"
)
```

設定。

---

# **3-8. Flyway Migration失敗**

## **症状**

```
Validate failed
```

---

## **原因**

- SQL変更履歴不一致
- Migration修正
- checksum不一致

---

## **対応方法**

開発環境のみ:

```bash
./gradlew flywayRepair
```

---

## **注意事項**

既存Migration直接修正禁止。

---

# **3-9. Port競合**

## **症状**

```
Port 8080 already in use
```

---

## **確認方法**

```bash
# Windows
netstat -ano | findstr 5173

# Mac
lsof -i :5173
```

---

## **Process終了**

```bash
#Windows
taskkill /f /pid PID

# Mac
kill -9 PID
```

---

# **3-10. `function lower(bytea) does not exist`**

## **症状**

`@Query`でnull許容の検索パラメータを`(:param IS NULL OR LOWER(...) LIKE ...)`パターンで使うJPQLが、パラメータが実際に`null`の場合にのみ500エラーになる。

```
ERROR: function lower(bytea) does not exist
```

`GET /api/v1/hospitals`（keyword・area省略時）で発見（2026-08-09）。単体テストはRepositoryをモックしているため検出できず、実際にDBへ接続するまで気づけなかった。

---

## **原因**

Hibernate 6がnull値のバインドパラメータの型を推論できず、PostgreSQL JDBCドライバがデフォルトの`bytea`型として送信してしまう。

---

## **対応方法**

該当パラメータをJPQLで明示的にキャストする。

```java
@Query("""
        SELECT h FROM HospitalEntity h
        WHERE (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        """)
```

---

## **教訓**

Service層のモックテストだけでは検出できないため、null許容パラメータを含む`@Query`には最低限1件、実DBに接続する統合テスト（`@DataJpaTest`等）を追加することが望ましい。

---

# **3-11. モックテストを通過する競合状態（2026-09-13 コードレビュー指摘）**

## **症状**

単体テストは全件green、手動操作でも再現しないが、同時リクエストで以下が成立してしまう。

| 事象 | 内容 |
| --- | --- |
| Refresh Token二重使用 | 2つのリクエストが同じ未失効Tokenを読み、両方が後続Tokenを発行できる |
| ログアウトしてもセッションが残る | 再発行が旧Tokenを失効させてから後続Tokenをinsertする間にログアウトの一括revokeが走ると、後続Tokenがログアウトのスナップショットに入らず失効対象から漏れる（同様にログインと競合すると有効Tokenが2本残る） |
| Refresh Token重複 | 同一ユーザー・同一秒の再発行で同じ署名文字列となり、`token`のUNIQUE制約違反で500 |
| 二重予約 | 両方が`existsActiveByScheduleId=false`を観測し、同一予約枠に2件のREQUESTEDが作成される |
| 終了状態の上書き | 承認と却下が同時に走ると、後から保存した側がREJECTEDをAPPROVEDで上書きする |

---

## **原因**

「読み取り → アプリ側で判定 → 保存」の3ステップが原子的でない。トランザクション境界（`@Transactional`）はステップ全体の排他を保証しない。また、条件付きUPDATEの戻り値（更新件数）を捨てていたため、0件更新でも処理が続行していた。

---

## **対応方法**

判定をDB側の1文に寄せ、更新件数で勝敗を判定する。

| 対象 | 対応 |
| --- | --- |
| Refresh Token消費 | `UPDATE ... WHERE token=? AND revoked=false`の更新件数が1件の呼び出しのみ後続発行を許可（0件はAUTH-003） |
| ログイン／再発行／ログアウトの競合 | 3操作ともTokenを触る前に対象ユーザー行を`SELECT ... FOR UPDATE`で排他取得し、ユーザー単位で直列化する（`09_Security_Design` §3-5） |
| Refresh Token重複 | 発行ごとに`jti`（UUID）を付与 |
| 二重予約 | 部分UNIQUE INDEX `uq_reservations_active_schedule`（`06_ERD` §17）を追加し、制約違反をRESERVATION-001に変換 |
| 状態遷移 | `UPDATE ... WHERE id=? AND status=<読み取り時点の状態>`の条件付き更新。0件はRESERVATION-008 |

---

## **教訓**

Repositoryをモックした単体テストは「同時実行」を一切検証しない。`docs/12` §3-10（実DB接続でしか出ないSQLの問題）と同種で、モックが通ることは正しさの根拠にならない。一意制約・条件付きUPDATEの更新件数など、**DBが保証する不変条件**として表現できるものはDB側に寄せる。なお本項の修正後も、実PostgreSQLでの同時実行テストは未実施（残課題）。

---

# **3-12. 読み取ったエンティティ全体の保存による上書き（2026-10-03 コードレビュー指摘）**

## **症状**

| 事象 | 内容 |
| --- | --- |
| PATCHで項目が消える | 健康記録に`{"memo":"..."}`だけを送ると、体重・症状がNULLになる |
| 削除した記録・ペットが復活する | PATCHが行を読んだ後に論理削除がcommitされると、PATCHの保存で`deleted_at`がNULLに戻り一覧に再表示される |
| 停止した病院がACTIVEに戻る | 病院情報の更新が、同時に行われた停止を`status=ACTIVE`で上書きする（逆に停止が情報更新を古い値で上書きする） |
| 削除した営業時間が作り直される | 物理削除後に古いエンティティを`save()`（merge）すると行が作り直され得る |

---

## **原因**

更新処理が「行を読み取る → ドメインで値を変える → エンティティ全体を`save()`する」構造だった。

- 全体保存は、その操作が変更しない列（`status`・`deleted_at`など）まで**読み取り時点の値で書き戻す**。読み取りから書き込みまでの間にcommitされた別の更新は消える。
- PATCHのDTOで未指定の項目はnullになり、それをそのまま渡していたため、部分更新が全体置換になっていた。
- 単体テストはRepositoryをモックしており、いずれも検出できなかった（§3-11と同種）。

---

## **対応方法**

| 対象 | 対応 |
| --- | --- |
| 共通 | 更新は**その操作が所有する列だけ**を`@Modifying`の条件付きUPDATEで書き、更新件数が0なら「存在しない」として扱う |
| 健康記録 | 内容列だけを`WHERE id=? AND deleted_at IS NULL`で更新。未指定項目は既存値を維持（`HealthRecord.patch`）。読み取りは行ロック付き（下記「追記」） |
| ペット | プロフィール列だけを`WHERE id=? AND deleted_at IS NULL`で更新 |
| 病院 | 情報更新（name/address/phone）と停止（status）を別々のUPDATEに分離 |
| 営業時間 | 時間列だけを`WHERE id=? AND hospital_id=?`で更新（mergeで行を作り直さない） |

予約枠のBLOCKED/AVAILABLE切替は status 1列だけを変える操作で、他の列を書く操作や削除が無いため対象外とした。

---

## **教訓**

`save()`による全体保存は「読み取ってから書くまでの間、誰もその行を変えない」ことを暗黙に仮定している。状態列・論理削除列を持つエンティティでは、その仮定は同時実行で簡単に崩れる。更新は「変更する列」と「前提条件（`deleted_at IS NULL`など）」を明示したUPDATE文で行う。

---

## **追記: 部分更新の同時実行で未指定項目が戻る（2026-10-04 再レビュー指摘）**

条件付きUPDATEにした後も、健康記録のPATCHは**未指定項目の値を読み取った行から補って**全内容列を書いていた。memo更新がcommitされるまで待たされたweightのみのPATCHは、待機前に読んだ古いmemoを書き戻し、受け取っていない項目を消していた（最終状態`5.30/original`、正しくは`5.30/concurrent-memo`）。

条件付きUPDATEが守るのは`WHERE`に書いた前提条件だけで、**読み取りから作った値で書く列**は守らない。部分更新を読み取りとのマージで行う場合は、読み取りを行ロック付き（`@Lock(PESSIMISTIC_WRITE)`、PostgreSQLでは`FOR NO KEY UPDATE`）にして読み取り〜書き込みを直列化する。指定された列だけをSQLで書く方法もあるが、空文字で消去する規則をSQLへ持ち込むことになるため、本プロジェクトは行ロックを選んだ。

病院情報とペットの更新は必須項目を含む全体置換で、未指定項目を読み取り値から補わないため対象外。

---

# **3-13. 日本時間0〜9時に「今日」が前日になる（2026-10-04 再レビュー指摘）**

## **症状**

| 事象 | 内容 |
| --- | --- |
| スモークテストの失敗 | 記録日の既定値（サーバーの当日）を`new Date().toISOString()`の日付と比較し、日本時間0時台に失敗した |
| 画面の日付 | 健康記録フォームの初期日付と予約日付の下限が、日本時間0〜9時に前日になる |

---

## **原因**

`toISOString()`は常にUTCで表すため、UTC+9の0〜9時は前日の日付になる。また、Spring APIの日付はJVMの既定タイムゾーンに依存しており、`TZ`未設定（UTC）のコンテナで動かすとサーバーの「当日」もずれる状態だった（`ServerTime`はAsia/Seoulを定義していたが未使用）。

---

## **対応方法**

| 対象 | 対応 |
| --- | --- |
| Spring API | `SpringApiApplication.main`でJVMの既定タイムゾーンを`ServerTime.ZONE_ID`（Asia/Tokyo）に固定 |
| フロントエンド | 「今日」は`todayInJapan()`（`src/utils/date.ts`、`Intl.DateTimeFormat`でAsia/Tokyo）で計算 |
| テスト | 期待値もAsia/Tokyoで計算し、リクエスト中に日付が変わる場合はどちらの日付も許容 |

日本と韓国はどちらもUTC+9で夏時間がないため、既存データの時刻はずれない。

---

# **4. Django関連**

# **4-1. Python Version不一致**

## **症状**

```
ModuleNotFoundError
```

---

## **原因**

pyenv Version不一致。

---

## **確認方法**

```bash
python --version
```

---

## **推奨Version**

```
Python 3.12.10
```

---

# **4-2. pyenv認識しない**

## **原因**

.zshrc設定未適用。

---

## **対応方法**

```bash
# Mac
source ~/.zshrc
```

---

# **4-3. Django Server起動失敗**

## **症状**

```bash
python manage.py runserver
```

失敗。

---

## **原因**

- venv未有効化
- package未install
- DB接続失敗

---

## **対応方法**

```bash
# Mac
source .venv/bin/activate

pip install -r requirements.txt
```

---

# **4-4. DRF Import Error**

## **症状**

```
No module named rest_framework
```

---

## **対応方法**

```bash
pip install djangorestframework
```

---

# **5. PostgreSQL関連**

# **5-1. DB接続失敗**

## **症状**

```
Connection refused
```

---

## **原因**

- PostgreSQL未起動
- Docker Container停止
- Port誤設定

---

## **対応方法**

```bash
docker ps
```

確認。

---

# **5-2. Password認証失敗**

## **症状**

```
password authentication failed
```

---

## **原因**

DB Username / Password不一致。

---

## **確認対象**

```
DB_USERNAME
DB_PASSWORD
```

---

# **5-3. Relation does not exist**

## **症状**

```
relation xxx does not exist
```

---

## **原因**

Migration未実行。

---

## **対応方法**

```bash
./gradlew flywayMigrate
```

---

# **6. Docker関連**

# **6-1. Docker Compose起動失敗**

## **症状**

```bash
docker compose up
```

失敗。

---

## **原因**

- Docker Desktop未起動
- Port競合
- YAML構文エラー

---

## **対応方法**

```bash
docker compose config
```

でYAML確認。

---

# **6-2. Container間通信失敗**

## **症状**

Backend → DB接続不可。

---

## **原因**

localhost利用。

---

## **対応方法**

Docker内部ではService名利用。

```
DB_HOST=postgres
```

---

# **6-3. Volume問題**

## **症状**

DB初期化される。

---

## **原因**

Volume未設定。

---

## **対応方法**

```yaml
volumes:
  postgres-data:
```

設定。

---

# **7. Security関連**

# **7-1. 403 Forbidden**

## **原因**

Role権限不足。

---

## **確認対象**

- JWT Role
- Spring Security
- API Authorization

---

# **7-2. CSRF Error**

## **原因**

CSRF設定競合。

---

## **対応方法**

JWT認証利用時:

```java
// securityConfig
csrf.disable()
```

---

# **7-3. Password Encode Error**

## **症状**

ログイン失敗。

---

## **原因**

BCrypt未適用。

---

## **対応方法**

```java
passwordEncoder.encode()
```

利用。

---

# **8. Scheduler関連**

# **8-1. Scheduler実行されない**

## **原因**

@EnableScheduling未設定。

---

## **対応方法**

```java
@EnableScheduling
```

追加。

---

# **8-2. 通知重複生成**

## **原因**

既存通知確認なし。

---

## **対応方法**

通知生成前に存在確認。

---

# **9. Git関連**

# **9-1. branch push失敗**

## **原因**

Remote同期不一致。

---

## **対応方法**

```bash
git pull origin develop
```

後再push。

---

# **9-2. LF / CRLF Warning**

## **原因**

OS改行コード差異。

---

## **対応方法**

```bash
git config --global core.autocrlf input
```

---

# **10. IntelliJ関連**

# **10-1. Gradle認識失敗**

## **原因**

Gradle Reload未実施。

---

## **対応方法**

```
Reload All Gradle Projects
```

実行。

---

# **10-2. Lombok認識しない**

## **原因**

Plugin未install。

---

## **対応方法**

- Lombok Plugin install
- Annotation Processing有効化

---

# **10-3. Java Version不一致**

## **原因**

Project SDK mismatch。

---

## **推奨設定**

| **項目** | **Version** |
| --- | --- |
| SDK | Java 17 |
| Gradle JVM | Java 17 |

---

# **11. VSCode関連**

# **11-1. Python Interpreter認識失敗**

## **原因**

venv未選択。

---

## **対応方法**

```
Python: Select Interpreter
```

から `.venv` 選択。

---

# **12. Logging確認方法**

# **12-1. Spring Boot Log**

```bash
tail -f logs/application.log
```

---

# **12-2. Docker Log**

```bash
docker logs container-name
```

---

# **12-3. PostgreSQL Log**

```bash
docker logs postgres
```

---

# **13. Build確認コマンド**

# **13-1. Spring Boot**

```bash
./gradlew clean build
```

---

# **13-2. Frontend**

```bash
npm run build
```

---

# **13-3. Django**

```bash
python manage.py check
```

---

# **14. 障害調査優先順位**

```mermaid
flowchart TB

	A["Error確認"]

	B["Log確認"]

	C["Environment確認"]

	D["Version確認"]

	E["Config確認"]

	F["Code確認"]

	A --> B --> C --> D --> E --> F
```

---

# **15. 設計上注意事項**

## **Migration直接修正禁止**

適用済Migrationは直接修正しない。

---

## **Version整合性維持**

Spring Boot / springdoc / Java Version整合性を維持する。

---

## **Docker内部通信**

Container内部ではlocalhostを利用しない。

---

## **Security設定確認**

JWT / Role / CORS設定を優先確認する。

---

# **16. 関連ドキュメント**

| **ドキュメント** | **内容** |
| --- | --- |
| [09_Security_Design](09_Security_Design%20366d097bd4f78074af53e3854d02bdc6.md)  | セキュリティ設計 |
| [10_Development_Environment](10_Development_Environment%20366d097bd4f78035ba4aef5e47b56378.md)  | 開発環境 |
| [11_Test_Strategy](11_Test_Strategy%20366d097bd4f7806abc43d3ba52e7a24d.md)  | テスト戦略 |
| [07_API_Design](07_API_Design%20363d097bd4f78052a200e297abde89f3.md)  | API設計 |
| [08_State_Design](08_State_Design%20365d097bd4f7808f9b01de0aa34fa730.md)  | 状態設計 |

---