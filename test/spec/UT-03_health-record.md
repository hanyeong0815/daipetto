# 単体テスト仕様書・結果 UT-03 健康記録

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 健康記録（docs/07 §6-1〜6-4） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 12/12 OK、自動テスト（JUnit）27/27 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-03-001 | `POST /api/v1/pets/{petId}/health-records` | 正常 | 健康記録の登録（記録日省略） | 体重・症状・メモ、recordedDate省略 | 201、healthRecordIdを返し、記録日は当日（JST） | OK | [UT-03-001.json](../evidence/api/UT-03-001.json) |
| UT-03-002 | `POST /api/v1/pets/{petId}/health-records` | 正常 | 過去日の記録 | recordedDate = 30日前 | 201、指定した記録日で保存される | OK | [UT-03-002.json](../evidence/api/UT-03-002.json) |
| UT-03-003 | `POST /api/v1/pets/{petId}/health-records` | 異常 | 体重の不正値 | weight = -0.5 | 400 VALIDATION-001 | OK | [UT-03-003.json](../evidence/api/UT-03-003.json) |
| UT-03-004 | `POST /api/v1/pets/{petId}/health-records` | 異常 | 他人のペットへの登録 | 他のユーザーのペットID | 403 HEALTH-002 | OK | [UT-03-004.json](../evidence/api/UT-03-004.json) |
| UT-03-005 | `GET /api/v1/pets/{petId}/health-records` | 正常 | 健康記録一覧の並び順 | 記録日の異なる3件 | 200、記録日の降順 | OK | [UT-03-005.json](../evidence/api/UT-03-005.json) |
| UT-03-006 | `GET /api/v1/pets/{petId}/health-records` | 異常 | 他人のペットの一覧 | 他のユーザーのペットID | 403 HEALTH-002 | OK | [UT-03-006.json](../evidence/api/UT-03-006.json) |
| UT-03-007 | `PATCH /api/v1/health-records/{healthRecordId}` | 正常 | 部分更新 | 体重4.5・症状「咳」の記録に、症状だけを送る | 200、症状だけ変わり、体重は維持される | OK | [UT-03-007.json](../evidence/api/UT-03-007.json) |
| UT-03-008 | `PATCH /api/v1/health-records/{healthRecordId}` | 異常 | 他人の記録の更新 | 他のユーザーのペットの記録 | 403 HEALTH-002、変更されない | OK | [UT-03-008.json](../evidence/api/UT-03-008.json) |
| UT-03-009 | `PATCH /api/v1/health-records/{healthRecordId}` | 異常 | 存在しない記録の更新 | 存在しない健康記録ID | 404 HEALTH-001 | OK | [UT-03-009.json](../evidence/api/UT-03-009.json) |
| UT-03-010 | `DELETE /api/v1/health-records/{healthRecordId}` | 正常 | 削除（論理削除） | 本人のペットの記録 | 200、一覧に出なくなり、もう一度削除すると404 HEALTH-001 | OK | [UT-03-010.json](../evidence/api/UT-03-010.json) |
| UT-03-011 | `DELETE /api/v1/health-records/{healthRecordId}` | 異常 | 他人の記録の削除 | 他のユーザーのペットの記録 | 403 HEALTH-002、削除されない | OK | [UT-03-011.json](../evidence/api/UT-03-011.json) |
| UT-03-012 | `PATCH /api/v1/health-records/{healthRecordId}` | 異常 | 更新時の体重の不正値（Codex G-03） | 本人の記録に weight = -0.01／weight = 1000（整数部4桁） | いずれも400 VALIDATION-001、記録は変更されない | OK | [UT-03-012.json](../evidence/api/UT-03-012.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | HEALTH-T001: 健康記録登録成功時、healthRecordIdを返却する | 合格 |
| 2 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | HEALTH-T002: 他ユーザーのペットへの登録時、HEALTH-002例外が発生する | 合格 |
| 3 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | HEALTH-T004: 健康記録一覧取得成功時、登録済データを返却する | 合格 |
| 4 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | HEALTH-T005: 健康記録削除時、deleted_atを設定する | 合格 |
| 5 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | memoだけを指定したPATCHでは、体重・症状・記録日が維持される（REVIEW-001 R-01） | 合格 |
| 6 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | recordedDate省略時、当日の日付で登録する | 合格 |
| 7 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | symptom・memoに空文字を指定すると消去（NULL）される | 合格 |
| 8 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 空のPATCHでは、どの項目も変更されない | 合格 |
| 9 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 健康記録は存在するがペットが取得できない場合、更新はHEALTH-002例外が発生する | 合格 |
| 10 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 健康記録は存在するがペットが取得できない場合、削除はHEALTH-002例外が発生する | 合格 |
| 11 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 健康記録更新成功時、指定した項目が反映される | 合格 |
| 12 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 存在しないペットへの登録時、HEALTH-002例外が発生する | 合格 |
| 13 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 存在しない健康記録の更新時、HEALTH-001例外が発生する | 合格 |
| 14 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 他ユーザーのペットの一覧取得時、HEALTH-002例外が発生する | 合格 |
| 15 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 他ユーザーのペットの記録更新時、HEALTH-002例外が発生する | 合格 |
| 16 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 他ユーザーのペットの記録削除時、HEALTH-002例外が発生する | 合格 |
| 17 | [HealthRecordServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.healthrecord.service.HealthRecordServiceTest.xml) | 読み取り後に論理削除がcommitされていた場合、更新は0件となりHEALTH-001例外が発生する（REVIEW-001 R-02） | 合格 |
| 18 | [HealthRecordMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.healthrecord.HealthRecordMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 19 | [HealthRecordMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.healthrecord.HealthRecordMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 20 | [HealthRecordPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.healthrecord.HealthRecordPersistenceAdapterTest.xml) | 排他取得は論理削除されていない記録だけを返す（REVIEW-002 R-04） | 合格 |
| 21 | [HealthRecordPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.healthrecord.HealthRecordPersistenceAdapterTest.xml) | 論理削除されていない記録は内容列が更新される | 合格 |
| 22 | [HealthRecordPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.healthrecord.HealthRecordPersistenceAdapterTest.xml) | 論理削除済みの記録は更新されず、deleted_atも維持される（REVIEW-001 R-02） | 合格 |
| 23 | [HealthRecordControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.healthrecord.HealthRecordControllerTest.xml) | DELETE /api/v1/health-records/{healthRecordId} - 健康記録論理削除成功 | 合格 |
| 24 | [HealthRecordControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.healthrecord.HealthRecordControllerTest.xml) | GET /api/v1/pets/{petId}/health-records - 健康記録一覧取得成功 | 合格 |
| 25 | [HealthRecordControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.healthrecord.HealthRecordControllerTest.xml) | HEALTH-T003: POST /api/v1/pets/{petId}/health-records - 体重が負数の場合Validation失敗 | 合格 |
| 26 | [HealthRecordControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.healthrecord.HealthRecordControllerTest.xml) | PATCH /api/v1/health-records/{healthRecordId} - 健康記録更新成功 | 合格 |
| 27 | [HealthRecordControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.healthrecord.HealthRecordControllerTest.xml) | POST /api/v1/pets/{petId}/health-records - 健康記録登録成功 | 合格 |
