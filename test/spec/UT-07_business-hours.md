# 単体テスト仕様書・結果 UT-07 診療時間（曜日別）

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 診療時間（曜日別）（docs/07 §8-7〜8-10） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 10/10 OK、自動テスト（JUnit）29/29 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-07-001 | `POST /api/v1/admin/hospitals/{hospitalId}/business-hours` | 正常 | 診療時間の登録 | 月曜 9:00〜18:00、休憩12:00〜13:00、30分単位 | 201、businessHoursIdを返す | OK | [UT-07-001.json](../evidence/api/UT-07-001.json) |
| UT-07-002 | `POST /api/v1/admin/hospitals/{hospitalId}/business-hours` | 異常 | 同じ曜日の重複登録 | 登録済みの曜日 | 409 HOSPITAL-004 | OK | [UT-07-002.json](../evidence/api/UT-07-002.json) |
| UT-07-003 | `POST /api/v1/admin/hospitals/{hospitalId}/business-hours` | 異常 | 診療時間の不正値 | 終了が開始より前／単位0分／休憩の片方だけ／休憩が営業時間外 | いずれも400 HOSPITAL-009 | OK | [UT-07-003.json](../evidence/api/UT-07-003.json) |
| UT-07-004 | `GET /api/v1/hospitals/{hospitalId}/business-hours` | 正常 | 診療時間一覧（一般ユーザー） | 月曜・水曜を登録した病院 | 200、登録した曜日の診療時間を返す。存在しない病院は404 HOSPITAL-001 | OK | [UT-07-004.json](../evidence/api/UT-07-004.json) |
| UT-07-005 | `PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | 正常 | 診療時間の更新 | 登録済みの月曜を10:00〜19:00、休憩なし、20分単位に変更 | 200、時間・単位が更新され、曜日は変わらない | OK | [UT-07-005.json](../evidence/api/UT-07-005.json) |
| UT-07-006 | `PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | 異常 | 別の病院の診療時間ID | パスの病院に属さない診療時間ID | 404 HOSPITAL-003、変更されない | OK | [UT-07-006.json](../evidence/api/UT-07-006.json) |
| UT-07-007 | `DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | 正常 | 診療時間の削除 | 登録済みの曜日 | 200、一覧から消える（休診扱い） | OK | [UT-07-007.json](../evidence/api/UT-07-007.json) |
| UT-07-008 | `POST /api/v1/admin/hospitals/{hospitalId}/business-hours` | 異常 | 紐付いていない病院の診療時間 | 病院管理者が紐付いていない病院 | 403 HOSPITAL-005 | OK | [UT-07-008.json](../evidence/api/UT-07-008.json) |
| UT-07-009 | `PATCH・DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | 異常 | 紐付いていない病院の診療時間の更新・削除（Codex G-08） | 病院1だけに紐付いた病院管理者が、病院2の診療時間を更新・削除／存在しない病院IDを指定 | いずれも403 HOSPITAL-005（所属の確認が先）。診療時間は変更・削除されない | OK | [UT-07-009.json](../evidence/api/UT-07-009.json) |
| UT-07-010 | `PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}` | 異常 | 更新時の診療時間の不正値（Codex G-09） | 終了が開始より前／単位241分／休憩が営業時間外 | いずれも400 HOSPITAL-009、診療時間は変更されない | OK | [UT-07-010.json](../evidence/api/UT-07-010.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 営業時間更新成功時、時間列だけを条件付きで更新する | 合格 |
| 2 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 営業時間削除成功時、リポジトリのdeleteByIdが呼ばれる | 合格 |
| 3 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 営業時間登録成功時、登録されたbusinessHoursIdを返却する | 合格 |
| 4 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 既に登録されている曜日への営業時間登録時、HOSPITAL-004例外が発生する | 合格 |
| 5 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 存在しない営業時間の更新時、HOSPITAL-003例外が発生する | 合格 |
| 6 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 存在しない営業時間の削除時、HOSPITAL-003例外が発生する | 合格 |
| 7 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 存在しない病院の営業時間一覧取得時、HOSPITAL-001例外が発生する | 合格 |
| 8 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 存在しない病院への営業時間登録時、HOSPITAL-001例外が発生する | 合格 |
| 9 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 他病院の営業時間を更新しようとした時、HOSPITAL-003例外が発生する | 合格 |
| 10 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 他病院の営業時間を削除しようとした時、HOSPITAL-003例外が発生する | 合格 |
| 11 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 読み取り後に営業時間が削除されていた場合、更新は0件となりHOSPITAL-003例外が発生する | 合格 |
| 12 | [HospitalBusinessHoursServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalBusinessHoursServiceTest.xml) | 病院営業時間一覧取得成功時、一覧を返却する | 合格 |
| 13 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 0時をまたぐ枠は作らない（LocalTimeの巻き戻りで無限ループしない） | 合格 |
| 14 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 営業開始〜休憩開始・休憩終了〜営業終了をAVAILABLE、休憩をBLOCKEDの枠に区切る | 合格 |
| 15 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 区間ごとに区切り、枠の長さに満たない残りは作らない | 合格 |
| 16 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 検証前に保存された営業時間外・一部だけ重なる・片方だけの休憩でも、営業時間外に枠を作らない（REVIEW-001 R-01） | 合格 |
| 17 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 検証前に保存された不正な値（枠0分・開始>=終了）では枠を作らない | 合格 |
| 18 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 登録・更新の検証: 開始<終了、枠5〜240分、休憩は両方指定かつ営業時間内（それ以外HOSPITAL-009） | 合格 |
| 19 | [HospitalBusinessHoursTest](../evidence/junit/TEST-koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHoursTest.xml) | 秒を含む営業時間・休憩の境界を切り捨てず、その時刻から区切る（REVIEW-001 R-02） | 合格 |
| 20 | [HospitalBusinessHoursAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalBusinessHoursAdapterTest.xml) | 営業時間の時間列が更新される | 合格 |
| 21 | [HospitalBusinessHoursAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalBusinessHoursAdapterTest.xml) | 削除済みの営業時間は更新されず、行も作り直されない | 合格 |
| 22 | [HospitalBusinessHoursAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalBusinessHoursAdapterTest.xml) | 別病院のhospitalIdを指定した更新は0件となる | 合格 |
| 23 | [HospitalBusinessHoursMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalBusinessHoursMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 24 | [HospitalBusinessHoursMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalBusinessHoursMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 25 | [HospitalBusinessHoursAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalBusinessHoursAdminControllerTest.xml) | DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId} - 営業時間削除成功 | 合格 |
| 26 | [HospitalBusinessHoursAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalBusinessHoursAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId} - 営業時間更新成功 | 合格 |
| 27 | [HospitalBusinessHoursAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalBusinessHoursAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/business-hours - dayOfWeekがnullの場合Validation失敗 | 合格 |
| 28 | [HospitalBusinessHoursAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalBusinessHoursAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/business-hours - 営業時間登録成功 | 合格 |
| 29 | [HospitalBusinessHoursControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalBusinessHoursControllerTest.xml) | GET /api/v1/hospitals/{hospitalId}/business-hours - 病院営業時間一覧取得成功 | 合格 |
