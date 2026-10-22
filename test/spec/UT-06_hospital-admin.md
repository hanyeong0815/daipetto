# 単体テスト仕様書・結果 UT-06 病院管理者の紐付け

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 病院管理者の紐付け（docs/07 §2-8・11-9〜11-11） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 8/8 OK、自動テスト（JUnit）8/8 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-06-001 | `POST /api/v1/admin/hospitals/{hospitalId}/admins` | 正常 | 紐付け | ROLE_HOSPITAL_ADMINのユーザー | 201、hospital_adminsに1件登録される | OK | [UT-06-001.json](../evidence/api/UT-06-001.json) |
| UT-06-002 | `POST /api/v1/admin/hospitals/{hospitalId}/admins` | 異常 | 紐付けの重複 | 既に紐付いているユーザー | 409 HOSPITAL-007 | OK | [UT-06-002.json](../evidence/api/UT-06-002.json) |
| UT-06-003 | `POST /api/v1/admin/hospitals/{hospitalId}/admins` | 異常 | 病院管理者権限のないユーザー | ROLE_USERのユーザー | 400 HOSPITAL-006 | OK | [UT-06-003.json](../evidence/api/UT-06-003.json) |
| UT-06-004 | `POST /api/v1/admin/hospitals/{hospitalId}/admins` | 異常 | 存在しない病院・ユーザー、userIdなし | 存在しない病院／存在しないユーザー／Request本文が{} | 404 HOSPITAL-001／404 USER-003／400 VALIDATION-001 | OK | [UT-06-004.json](../evidence/api/UT-06-004.json) |
| UT-06-005 | `POST /api/v1/admin/hospitals/{hospitalId}/admins` | 異常 | 権限（病院管理者による紐付け） | ROLE_HOSPITAL_ADMIN | 403 AUTH-005 | OK | [UT-06-005.json](../evidence/api/UT-06-005.json) |
| UT-06-006 | `GET /api/v1/admin/hospitals/{hospitalId}/admins` | 正常 | 病院管理者一覧 | 病院1（病院管理者1名が紐付き） | 200、userId・email・nicknameを返す。存在しない病院は404 HOSPITAL-001 | OK | [UT-06-006.json](../evidence/api/UT-06-006.json) |
| UT-06-007 | `DELETE /api/v1/admin/hospitals/{hospitalId}/admins/{userId}` | 正常 | 紐付け解除 | 紐付いた病院管理者 | 200、解除後はその病院を操作できず（403 HOSPITAL-005）、もう一度解除すると404 HOSPITAL-008 | OK | [UT-06-007.json](../evidence/api/UT-06-007.json) |
| UT-06-008 | `PATCH /api/v1/admin/users/{userId}/role` | 正常 | 権限変更による紐付けの削除 | 病院に紐付いた病院管理者を一般ユーザーに変更 | 紐付けがすべて削除され、病院管理者に戻しても復元されない | OK | [UT-06-008.json](../evidence/api/UT-06-008.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | システム管理者はすべての病院、病院管理者は紐付いた病院だけを管理できる（それ以外はHOSPITAL-005） | 合格 |
| 2 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | 管理対象の病院: システム管理者は停止中を含む全病院、病院管理者は紐付いた病院だけ | 合格 |
| 3 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | 存在しないユーザーはAUTH-005 | 合格 |
| 4 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | 紐付けの解除。紐付けが無ければHOSPITAL-008 | 合格 |
| 5 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | 紐付けの失敗: 病院なしHOSPITAL-001・ユーザーなし/削除済みUSER-003・権限違いHOSPITAL-006・既存HOSPITAL-007 | 合格 |
| 6 | [HospitalAdminServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalAdminServiceTest.xml) | 病院管理者権限のユーザーを病院に紐付ける | 合格 |
| 7 | [HospitalAdminPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalAdminPersistenceAdapterTest.xml) | 同じ紐付けの二重登録は一意制約違反をHOSPITAL-007に変換する | 合格 |
| 8 | [HospitalAdminPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalAdminPersistenceAdapterTest.xml) | 紐付けの登録・存在確認・病院ごとの管理者（ユーザー結合）・ユーザーごとの病院ID・解除 | 合格 |
