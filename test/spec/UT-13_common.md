# 単体テスト仕様書・結果 UT-13 共通（認証・入力・応答形式）

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 共通（認証・入力・応答形式）（docs/07 §2・docs/09） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 8/8 OK、自動テスト（JUnit）5/5 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-13-001 | `GET /api/v1/pets（代表）` | 異常 | 未認証のアクセス | Authorizationヘッダーなし | 401 AUTH-001（共通エラー形式 success=false・code・message） | OK | [UT-13-001.json](../evidence/api/UT-13-001.json) |
| UT-13-002 | `POST /api/v1/pets（代表）` | 異常 | 読み取れないJSON | 壊れたJSON（閉じ括弧なし） | 400 VALIDATION-001（500にしない） | OK | [UT-13-002.json](../evidence/api/UT-13-002.json) |
| UT-13-003 | `GET /api/v1/pets/{petId}（代表）` | 異常 | パス変数の型不正 | petId = "abc" | 400 VALIDATION-001（500にしない） | OK | [UT-13-003.json](../evidence/api/UT-13-003.json) |
| UT-13-004 | `存在しないURL` | 異常 | 存在しないURL | GET /api/v1/unknown（認証済み） | 404 COMMON-001（共通エラー形式。500にしない、docs/07 §2-4） | OK | [UT-13-004.json](../evidence/api/UT-13-004.json) |
| UT-13-005 | `DELETE /api/v1/hospitals（代表）` | 異常 | 許可されていないHTTPメソッド | 定義されていないメソッド（DELETE /api/v1/hospitals） | 405 COMMON-002（共通エラー形式。500にしない） | OK | [UT-13-005.json](../evidence/api/UT-13-005.json) |
| UT-13-006 | `成功レスポンス（代表）` | 正常 | 共通の成功形式 | GET /api/v1/users/me | 200、success=true・dataを持つ共通形式 | OK | [UT-13-006.json](../evidence/api/UT-13-006.json) |
| UT-13-007 | `POST /api/v1/pets（代表）` | 異常 | 対応していないContent-Type | Content-Type: text/plain でJSON本文を送る | 415 COMMON-003（共通エラー形式。500にしない） | OK | [UT-13-007.json](../evidence/api/UT-13-007.json) |
| UT-13-008 | `管理系15エンドポイント` | 異常 | 権限（Role）不足の拒否（Codex G-07） | 一般ユーザー（ROLE_USER）で管理系の15エンドポイントを呼ぶ。そのうちシステム管理者専用の4つ（病院管理者一覧・紐付け解除・停止解除・権限変更）は病院管理者でも呼ぶ。IDとRequest本文は正しい値 | いずれも403 AUTH-005、データは書き込まれない（関連テーブルの行数・状態が変わらない） | OK | [UT-13-008.json](../evidence/api/UT-13-008.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HospitalControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalControllerTest.xml) | 許可されていないHTTPメソッドは500ではなく405（COMMON-002） | 合格 |
| 2 | [HospitalControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalControllerTest.xml) | 想定外の例外は内部情報を含まないSERVER-001の500で返す | 合格 |
| 3 | [HospitalControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalControllerTest.xml) | 存在しないURLは500ではなく404（COMMON-001） | 合格 |
| 4 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - Content-Typeがtext/plainの場合は415（COMMON-003）で、登録しない | 合格 |
| 5 | [SpringApiApplicationTests](../evidence/junit/TEST-koh.portfolio.springapi.SpringApiApplicationTests.xml) | contextLoads() | 合格 |
