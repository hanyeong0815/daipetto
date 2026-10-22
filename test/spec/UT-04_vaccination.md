# 単体テスト仕様書・結果 UT-04 ワクチン接種予定

| 項目 | 内容 |
| --- | --- |
| 対象機能 | ワクチン接種予定（docs/07 §6-5〜6-11） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 14/14 OK、自動テスト（JUnit）54/54 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-04-001 | `POST /api/v1/pets/{petId}/vaccinations` | 正常 | 接種予定の登録 | ワクチン名・接種予定日 | 201、vaccinationIdを返し、状態SCHEDULEDで保存される | OK | [UT-04-001.json](../evidence/api/UT-04-001.json) |
| UT-04-002 | `POST /api/v1/pets/{petId}/vaccinations` | 異常 | 入力チェック | ワクチン名が空／101文字／接種予定日なし | いずれも400 VALIDATION-001 | OK | [UT-04-002.json](../evidence/api/UT-04-002.json) |
| UT-04-003 | `POST /api/v1/pets/{petId}/vaccinations` | 異常 | 他人のペットへの登録 | 他のユーザーのペットID | 403 VACCINATION-002 | OK | [UT-04-003.json](../evidence/api/UT-04-003.json) |
| UT-04-004 | `GET /api/v1/pets/{petId}/vaccinations` | 正常 | 一覧の並び順 | 接種予定日の異なる3件 | 200、接種予定日の昇順 | OK | [UT-04-004.json](../evidence/api/UT-04-004.json) |
| UT-04-005 | `PATCH /api/v1/vaccinations/{vaccinationId}/complete` | 正常 | 接種完了（完了日省略） | SCHEDULEDの予定、Request本文なし | 200、COMPLETED、完了日は当日（JST） | OK | [UT-04-005.json](../evidence/api/UT-04-005.json) |
| UT-04-006 | `PATCH /api/v1/vaccinations/{vaccinationId}/complete` | 異常 | 未来日の完了日 | completedDate = 明日 | 400 VALIDATION-001 | OK | [UT-04-006.json](../evidence/api/UT-04-006.json) |
| UT-04-007 | `PATCH /api/v1/vaccinations/{vaccinationId}/complete` | 異常 | 不正な状態遷移（完了済みの完了） | COMPLETEDの予定 | 409 VACCINATION-003 | OK | [UT-04-007.json](../evidence/api/UT-04-007.json) |
| UT-04-008 | `PATCH /api/v1/vaccinations/{vaccinationId}/cancel` | 正常 | 接種予定の中止 | SCHEDULEDの予定 | 200、CANCELLED | OK | [UT-04-008.json](../evidence/api/UT-04-008.json) |
| UT-04-009 | `PATCH /api/v1/vaccinations/{vaccinationId}/cancel` | 異常 | 不正な状態遷移（完了済みの中止） | COMPLETEDの予定 | 409 VACCINATION-003 | OK | [UT-04-009.json](../evidence/api/UT-04-009.json) |
| UT-04-010 | `PATCH /api/v1/vaccinations/{vaccinationId}/reschedule` | 正常 | 中止した予定の再登録 | CANCELLEDの予定、新しい接種予定日 | 200、SCHEDULEDに戻り、新しい予定日になる | OK | [UT-04-010.json](../evidence/api/UT-04-010.json) |
| UT-04-011 | `PATCH /api/v1/vaccinations/{vaccinationId}/reschedule` | 異常 | 不正な状態遷移（予定中の再登録） | SCHEDULEDの予定 | 409 VACCINATION-003 | OK | [UT-04-011.json](../evidence/api/UT-04-011.json) |
| UT-04-012 | `PATCH /api/v1/vaccinations/{vaccinationId}/complete` | 異常 | 他人の予定の操作 | 他のユーザーのペットの予定 | 403 VACCINATION-002、変更されない | OK | [UT-04-012.json](../evidence/api/UT-04-012.json) |
| UT-04-013 | `DELETE /api/v1/vaccinations/{vaccinationId}` | 正常 | 接種予定の削除 | 本人のペットの予定 | 200、一覧に出なくなり、もう一度削除すると404 VACCINATION-001 | OK | [UT-04-013.json](../evidence/api/UT-04-013.json) |
| UT-04-014 | `PATCH /api/v1/vaccinations/{vaccinationId}/reschedule` | 異常 | 存在しない予定の再登録（Codex G-04） | 存在しない接種予定ID、正しい接種予定日 | 404 VACCINATION-001 | OK | [UT-04-014.json](../evidence/api/UT-04-014.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | CANCELLED→SCHEDULED: 新しい接種予定日で再登録する | 合格 |
| 2 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | COMPLETED・CANCELLEDからの接種完了はVACCINATION-003例外が発生する | 合格 |
| 3 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | COMPLETED・CANCELLEDからの中止はVACCINATION-003例外が発生する | 合格 |
| 4 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | SCHD-T002: 当日〜7日後の対象ごとにVACCINATION通知を作成する | 合格 |
| 5 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | SCHEDULED・COMPLETEDからの再登録はVACCINATION-003例外が発生する | 合格 |
| 6 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | SCHEDULED→CANCELLED: 遷移元条件付き更新を行う | 合格 |
| 7 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | SCHEDULED→COMPLETED: 指定した接種完了日で遷移元条件付き更新を行う | 合格 |
| 8 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種完了日省略時、当日を接種完了日とする | 合格 |
| 9 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定は存在するがペットが取得できない場合、完了はVACCINATION-002例外が発生する | 合格 |
| 10 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定は存在するがペットが取得できない場合、再登録はVACCINATION-002例外が発生する | 合格 |
| 11 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定は存在するがペットが取得できない場合、削除はVACCINATION-002例外が発生する | 合格 |
| 12 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定は存在するがペットが取得できない場合、中止はVACCINATION-002例外が発生する | 合格 |
| 13 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定一覧取得成功時、登録済データを返却する | 合格 |
| 14 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定削除時、論理削除を行う | 合格 |
| 15 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 接種予定登録成功時、SCHEDULEDで保存しvaccinationIdを返却する | 合格 |
| 16 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 存在しないペットの一覧取得時、VACCINATION-002例外が発生する | 合格 |
| 17 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 存在しない接種予定の完了時、VACCINATION-001例外が発生する | 合格 |
| 18 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 存在しない接種予定の中止時、VACCINATION-001例外が発生する | 合格 |
| 19 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 他ユーザーのペットの再登録時、VACCINATION-002例外が発生する | 合格 |
| 20 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 他ユーザーのペットの接種完了時、VACCINATION-002例外が発生する | 合格 |
| 21 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 他ユーザーのペットの接種予定削除時、VACCINATION-002例外が発生する | 合格 |
| 22 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 他ユーザーのペットの中止時、VACCINATION-002例外が発生する | 合格 |
| 23 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 他ユーザーのペットへの登録時、VACCINATION-002例外が発生する | 合格 |
| 24 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 対象が無い場合、通知を作成しない | 合格 |
| 25 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 抽出後に中止・再登録などが確定し送信済みにできない対象は通知しない（REVIEW-001 R-03） | 合格 |
| 26 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 読み取り後に削除済みとなり更新0件の場合、VACCINATION-001例外が発生する | 合格 |
| 27 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 読み取り後に別の遷移が確定し更新0件の場合、VACCINATION-003例外が発生する | 合格 |
| 28 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 読み取り後に別の遷移が確定し更新0件の場合、再登録はVACCINATION-003例外が発生する | 合格 |
| 29 | [VaccinationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.vaccination.service.VaccinationServiceTest.xml) | 読み取り後に別の遷移が確定し更新0件の場合、中止はVACCINATION-003例外が発生する | 合格 |
| 30 | [VaccinationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 31 | [VaccinationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 32 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | SCHD-T001/T003/T004: 7日以内のSCHEDULEDだけを飼い主付きで抽出する | 合格 |
| 33 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | SCHD-T005: 送信済みにした予定は再び抽出せず、二度目の送信済み化は失敗する | 合格 |
| 34 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 再登録はCANCELLEDの行だけを新しい予定日でSCHEDULEDに戻す | 合格 |
| 35 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 接種完了はSCHEDULEDの行だけを更新する | 合格 |
| 36 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 送信済みの予定を中止・再登録すると、新しい予定日で再び抽出される | 合格 |
| 37 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 中止はSCHEDULEDの行だけをCANCELLEDにし、updated_atを進める | 合格 |
| 38 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 抽出後に中止・再登録が確定した場合、抽出時の予定日では送信済みにできず、新しい予定日は未送信のまま残る（REVIEW-001 R-03） | 合格 |
| 39 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 論理削除されたペットの接種予定は抽出しない | 合格 |
| 40 | [VaccinationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.vaccination.VaccinationPersistenceAdapterTest.xml) | 論理削除は未削除の行だけを対象とし、削除後は一覧・リマインダー対象から外れる | 合格 |
| 41 | [VaccinationReminderSchedulerTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.scheduler.VaccinationReminderSchedulerTest.xml) | remindは毎日9時（JST）にスケジュールされる | 合格 |
| 42 | [VaccinationReminderSchedulerTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.scheduler.VaccinationReminderSchedulerTest.xml) | remind実行時、JSTの当日を指定してユースケースを1回だけ呼び出す | 合格 |
| 43 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | DELETE /api/v1/vaccinations/{id} - 削除成功 | 合格 |
| 44 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | DELETE /api/v1/vaccinations/{id} - 他ユーザーのペットは403とVACCINATION-002を返す | 合格 |
| 45 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | GET /api/v1/pets/{petId}/vaccinations - 接種予定一覧取得成功 | 合格 |
| 46 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/cancel - 中止成功 | 合格 |
| 47 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/cancel - 不正な状態遷移は409とVACCINATION-003を返す | 合格 |
| 48 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/complete - body省略時は完了日nullでUseCaseを呼ぶ | 合格 |
| 49 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/complete - 不正な状態遷移は409とVACCINATION-003を返す | 合格 |
| 50 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/complete - 未来の接種完了日はValidation失敗 | 合格 |
| 51 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/reschedule - 再登録成功 | 合格 |
| 52 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | PATCH /api/v1/vaccinations/{id}/reschedule - 接種予定日が無い場合Validation失敗 | 合格 |
| 53 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | POST /api/v1/pets/{petId}/vaccinations - ワクチン名・接種予定日が無い場合Validation失敗 | 合格 |
| 54 | [VaccinationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.vaccination.VaccinationControllerTest.xml) | POST /api/v1/pets/{petId}/vaccinations - 接種予定登録成功 | 合格 |
