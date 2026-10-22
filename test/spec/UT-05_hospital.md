# 単体テスト仕様書・結果 UT-05 病院

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 病院（docs/07 §8-1・8-2・11-1〜11-3・11-8） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 11/11 OK、自動テスト（JUnit）27/27 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-05-001 | `POST /api/v1/admin/hospitals` | 正常 | 病院登録（システム管理者） | 名前・住所・電話番号 | 201、hospitalIdを返し、状態ACTIVEで登録される | OK | [UT-05-001.json](../evidence/api/UT-05-001.json) |
| UT-05-002 | `POST /api/v1/admin/hospitals` | 異常 | 必須項目なし | 病院名なし | 400 VALIDATION-001 | OK | [UT-05-002.json](../evidence/api/UT-05-002.json) |
| UT-05-003 | `POST /api/v1/admin/hospitals` | 異常 | 権限（病院管理者による登録） | ROLE_HOSPITAL_ADMIN | 403 AUTH-005 | OK | [UT-05-003.json](../evidence/api/UT-05-003.json) |
| UT-05-004 | `GET /api/v1/hospitals` | 正常 | 病院一覧（検索条件・停止中の除外） | 名前・住所の異なる営業中の病院と停止中の病院 | 200、営業中の病院だけを返し、keyword（病院名）・area（住所）で絞り込める | OK | [UT-05-004.json](../evidence/api/UT-05-004.json) |
| UT-05-005 | `GET /api/v1/hospitals/{hospitalId}` | 正常 | 病院詳細 | 登録済みの病院 | 200、名前・住所・状態・createdAt（+09:00）を返す | OK | [UT-05-005.json](../evidence/api/UT-05-005.json) |
| UT-05-006 | `GET /api/v1/hospitals/{hospitalId}` | 異常 | 存在しない病院 | 存在しない病院ID | 404 HOSPITAL-001 | OK | [UT-05-006.json](../evidence/api/UT-05-006.json) |
| UT-05-007 | `PATCH /api/v1/admin/hospitals/{hospitalId}` | 正常 | 病院更新（紐付いた病院管理者） | 病院管理者が紐付いた病院の名前・住所・電話番号を変更 | 200、情報列だけが更新され、状態は変わらない | OK | [UT-05-007.json](../evidence/api/UT-05-007.json) |
| UT-05-008 | `PATCH /api/v1/admin/hospitals/{hospitalId}` | 異常 | 紐付いていない病院の更新 | 病院管理者が紐付いていない病院 | 403 HOSPITAL-005、変更されない | OK | [UT-05-008.json](../evidence/api/UT-05-008.json) |
| UT-05-009 | `PATCH /api/v1/admin/hospitals/{hospitalId}/suspend` | 正常 | 病院停止（システム管理者） | 営業中の病院 | 200、SUSPENDEDになり、一般向けの一覧から消え、詳細は状態SUSPENDEDで取得できる | OK | [UT-05-009.json](../evidence/api/UT-05-009.json) |
| UT-05-010 | `PATCH /api/v1/admin/hospitals/{hospitalId}/suspend` | 異常 | 権限・存在チェック | 病院管理者による停止／存在しない病院の停止（システム管理者） | 403 AUTH-005／404 HOSPITAL-001 | OK | [UT-05-010.json](../evidence/api/UT-05-010.json) |
| UT-05-011 | `GET /api/v1/admin/hospitals/mine` | 正常 | 管理対象の病院一覧 | システム管理者／病院1だけに紐付いた病院管理者 | 200。システム管理者は停止中を含む全病院、病院管理者は紐付いた病院だけ | OK | [UT-05-011.json](../evidence/api/UT-05-011.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 管理対象外の病院の情報更新はHOSPITAL-005となり、読み取り・更新を行わない | 合格 |
| 2 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 存在しない病院の詳細取得時、HOSPITAL-001例外が発生する | 合格 |
| 3 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 存在しない病院更新時、HOSPITAL-001例外が発生する | 合格 |
| 4 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 存在しない病院停止時、HOSPITAL-001例外が発生する | 合格 |
| 5 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 読み取り後に病院が無くなっていた場合、情報更新は0件となりHOSPITAL-001例外が発生する | 合格 |
| 6 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 病院一覧取得成功時、keyword・areaをリポジトリに渡し一覧を返却する | 合格 |
| 7 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 病院詳細取得成功時、病院情報を返却する | 合格 |
| 8 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 病院情報更新成功時、情報列だけを更新しstatusは書き込まない | 合格 |
| 9 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 病院停止成功時、status列だけをSUSPENDEDに更新し情報列は書き込まない | 合格 |
| 10 | [HospitalServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalServiceTest.xml) | 病院登録成功時、登録されたhospitalIdを返却する | 合格 |
| 11 | [HospitalMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 12 | [HospitalMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 13 | [HospitalPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalPersistenceAdapterTest.xml) | 情報更新はstatusを書き込まない。停止済みの病院がACTIVEに戻らない（REVIEW-001 R-03） | 合格 |
| 14 | [HospitalPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalPersistenceAdapterTest.xml) | 存在しない病院の更新は0件でfalseを返却する | 合格 |
| 15 | [HospitalPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalPersistenceAdapterTest.xml) | 停止はstatusだけを書き込み、同時に更新された情報を古い値で上書きしない | 合格 |
| 16 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | GET /api/v1/admin/hospitals/mine - 操作者IDで管理対象の病院を返す | 合格 |
| 17 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | GET・POST・DELETE /api/v1/admin/hospitals/{id}/admins - 病院管理者の一覧・紐付け・解除 | 合格 |
| 18 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId} - nameが空白の場合Validation失敗 | 合格 |
| 19 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId} - 病院情報更新成功 | 合格 |
| 20 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId}/suspend - 病院停止成功 | 合格 |
| 21 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{id} - 管理対象外の病院は403（HOSPITAL-005） | 合格 |
| 22 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | POST /api/v1/admin/hospitals - addressが空白の場合Validation失敗 | 合格 |
| 23 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | POST /api/v1/admin/hospitals - nameが空白の場合Validation失敗 | 合格 |
| 24 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | POST /api/v1/admin/hospitals - 病院登録成功 | 合格 |
| 25 | [HospitalAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{id}/admins - userIdなしは400、重複は409（HOSPITAL-007） | 合格 |
| 26 | [HospitalControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalControllerTest.xml) | GET /api/v1/hospitals - 病院一覧取得成功 | 合格 |
| 27 | [HospitalControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalControllerTest.xml) | GET /api/v1/hospitals/{hospitalId} - 病院詳細取得成功 | 合格 |
