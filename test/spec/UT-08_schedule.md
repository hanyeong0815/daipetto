# 単体テスト仕様書・結果 UT-08 予約枠

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 予約枠（docs/07 §8-3〜8-6・8-11） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 10/10 OK、自動テスト（JUnit）29/29 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-08-001 | `POST /api/v1/admin/hospitals/{hospitalId}/schedules` | 正常 | 予約枠の個別登録 | 日付・開始時刻・終了時刻（紐付いた病院管理者） | 201、scheduleIdを返し、AVAILABLEで登録される | OK | [UT-08-001.json](../evidence/api/UT-08-001.json) |
| UT-08-002 | `POST /api/v1/admin/hospitals/{hospitalId}/schedules` | 異常 | 必須項目・存在しない病院 | 日付なし／存在しない病院（システム管理者） | 400 VALIDATION-001／404 HOSPITAL-001 | OK | [UT-08-002.json](../evidence/api/UT-08-002.json) |
| UT-08-003 | `GET /api/v1/hospitals/{hospitalId}/schedules` | 正常 | 予約枠一覧（一般ユーザー） | 日付・時刻の異なる予約枠 | 200、日付・開始時刻の昇順で、AVAILABLE・BLOCKEDの両方を返す | OK | [UT-08-003.json](../evidence/api/UT-08-003.json) |
| UT-08-004 | `PATCH .../schedules/{scheduleId}/block, /unblock` | 正常 | BLOCKED・AVAILABLEの切り替え | AVAILABLEの予約枠 | blockで200・BLOCKED、unblockで200・AVAILABLE | OK | [UT-08-004.json](../evidence/api/UT-08-004.json) |
| UT-08-005 | `PATCH .../schedules/{scheduleId}/block` | 異常 | 別の病院の予約枠 | パスの病院に属さない予約枠ID（システム管理者） | 404 HOSPITAL-002、変更されない | OK | [UT-08-005.json](../evidence/api/UT-08-005.json) |
| UT-08-006 | `POST .../schedules (block)` | 異常 | 紐付いていない病院の予約枠 | 病院管理者が紐付いていない病院への登録・block | いずれも403 HOSPITAL-005 | OK | [UT-08-006.json](../evidence/api/UT-08-006.json) |
| UT-08-007 | `POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate` | 正常 | 予約枠の自動生成 | 月曜 9:00〜11:00（休憩10:00〜10:30、30分単位）だけを登録した病院で、翌月を指定 | 200。月曜ごとに4枠（09:00・09:30・10:30はAVAILABLE、休憩の10:00はBLOCKED）、作成数・日数が暦と一致する | OK | [UT-08-007.json](../evidence/api/UT-08-007.json) |
| UT-08-008 | `POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate` | 正常 | 同じ月の再実行 | UT-08-007と同じ条件で2回実行 | 2回目は作成0件、診療日はすべて対象外（skippedDates）として数え、枠は増えない | OK | [UT-08-008.json](../evidence/api/UT-08-008.json) |
| UT-08-009 | `POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate` | 異常 | 生成できない月・形式不正・権限 | 先月／13か月先／"2026/11"／紐付いていない病院管理者 | 400 HOSPITAL-010／400 HOSPITAL-010／400 VALIDATION-001／403 HOSPITAL-005 | OK | [UT-08-009.json](../evidence/api/UT-08-009.json) |
| UT-08-010 | `PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock` | 異常 | 紐付いていない病院の予約枠のAVAILABLE化（Codex G-08） | 病院2のBLOCKEDの予約枠を、病院1だけに紐付いた病院管理者がunblock | 403 HOSPITAL-005、BLOCKEDのまま | OK | [UT-08-010.json](../evidence/api/UT-08-010.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [HospitalScheduleGenerationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleGenerationServiceTest.xml) | 営業時間が未登録なら枠を作らない。存在しない病院はHOSPITAL-001で何も保存しない | 合格 |
| 2 | [HospitalScheduleGenerationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleGenerationServiceTest.xml) | 月次バッチ: 営業中の病院だけを月初〜月末で生成し、1病院の失敗で他を止めず作成数を合計する | 合格 |
| 3 | [HospitalScheduleGenerationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleGenerationServiceTest.xml) | 手動生成: 過去の月・13か月以上先はHOSPITAL-010、管理対象外はHOSPITAL-005で生成しない | 合格 |
| 4 | [HospitalScheduleGenerationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleGenerationServiceTest.xml) | 手動生成: 所属確認の後、翌月以降は1日から月末、今月は今日（JST）から月末で生成する | 合格 |
| 5 | [HospitalScheduleGenerationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleGenerationServiceTest.xml) | 病院行を排他取得してから、営業時間のある曜日だけ枠を作る。既に枠がある日は飛ばす | 合格 |
| 6 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 存在しない病院の予約枠一覧取得時、HOSPITAL-001例外が発生する | 合格 |
| 7 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 存在しない病院への予約枠登録時、HOSPITAL-001例外が発生する | 合格 |
| 8 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 存在しない予約枠のAVAILABLE設定時、HOSPITAL-002例外が発生する | 合格 |
| 9 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 存在しない予約枠のBLOCKED設定時、HOSPITAL-002例外が発生する | 合格 |
| 10 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 他病院の予約枠をAVAILABLE設定しようとした時、HOSPITAL-002例外が発生する | 合格 |
| 11 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 他病院の予約枠をBLOCKED設定しようとした時、HOSPITAL-002例外が発生する | 合格 |
| 12 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 病院予約枠一覧取得成功時、一覧を返却する | 合格 |
| 13 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 予約枠AVAILABLE設定成功時、statusがAVAILABLEで保存される | 合格 |
| 14 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 予約枠BLOCKED設定成功時、statusがBLOCKEDで保存される | 合格 |
| 15 | [HospitalScheduleServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.hospital.service.HospitalScheduleServiceTest.xml) | 予約枠登録成功時、登録されたscheduleIdを返却する | 合格 |
| 16 | [HospitalScheduleMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalScheduleMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 17 | [HospitalScheduleMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalScheduleMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 18 | [HospitalSchedulePersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalSchedulePersistenceAdapterTest.xml) | 期間内で枠がある日を重複なく返し、一括保存した枠（BLOCKEDを含む）もその日として数える | 合格 |
| 19 | [HospitalSchedulePersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalSchedulePersistenceAdapterTest.xml) | 病院行の排他取得は論理削除されていない病院だけを返す | 合格 |
| 20 | [HospitalScheduleGenerationSchedulerTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.scheduler.HospitalScheduleGenerationSchedulerTest.xml) | generateNextMonthは毎月1日0時（JST）にスケジュールされる | 合格 |
| 21 | [HospitalScheduleGenerationSchedulerTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.scheduler.HospitalScheduleGenerationSchedulerTest.xml) | generateNextMonth実行時、JSTの翌月を指定してユースケースを1回だけ呼び出す | 合格 |
| 22 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/block - BLOCKED設定成功 | 合格 |
| 23 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock - AVAILABLE設定成功 | 合格 |
| 24 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/schedules - availableDateがnullの場合Validation失敗 | 合格 |
| 25 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/schedules - 予約枠登録成功 | 合格 |
| 26 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate - monthなし・形式誤りは400 | 合格 |
| 27 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate - 過去の月はHOSPITAL-010（400） | 合格 |
| 28 | [HospitalScheduleAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleAdminControllerTest.xml) | POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate - 月を指定して自動生成し、件数を返す | 合格 |
| 29 | [HospitalScheduleControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.hospital.HospitalScheduleControllerTest.xml) | GET /api/v1/hospitals/{hospitalId}/schedules - 病院予約枠一覧取得成功 | 合格 |
