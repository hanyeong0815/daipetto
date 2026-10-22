# 単体テスト仕様書・結果 UT-09 予約（利用者）

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 予約（利用者）（docs/07 §9-1〜9-4） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 13/13 OK、自動テスト（JUnit）32/32 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-09-001 | `POST /api/v1/reservations` | 正常 | 予約申請 | 本人のペット・営業中の病院の未来のAVAILABLE枠・メモ | 201、reservationIdを返し、状態REQUESTED | OK | [UT-09-001.json](../evidence/api/UT-09-001.json) |
| UT-09-002 | `POST /api/v1/reservations` | 異常 | 同じ予約枠への重複予約 | 申請中の予約がある枠に別ユーザーが申請 | 409 RESERVATION-001 | OK | [UT-09-002.json](../evidence/api/UT-09-002.json) |
| UT-09-003 | `POST /api/v1/reservations` | 異常 | BLOCKEDの予約枠 | BLOCKEDにした予約枠 | 409 RESERVATION-003 | OK | [UT-09-003.json](../evidence/api/UT-09-003.json) |
| UT-09-004 | `POST /api/v1/reservations` | 異常 | 他人のペットでの申請 | 他のユーザーのペットID | 403 RESERVATION-004 | OK | [UT-09-004.json](../evidence/api/UT-09-004.json) |
| UT-09-005 | `POST /api/v1/reservations` | 異常 | 過去の予約枠 | 昨日の日付の予約枠（DBに直接作成） | 400 RESERVATION-005 | OK | [UT-09-005.json](../evidence/api/UT-09-005.json) |
| UT-09-006 | `POST /api/v1/reservations` | 異常 | 病院に属さない予約枠 | 病院2の予約枠を病院1として申請 | 404 RESERVATION-002 | OK | [UT-09-006.json](../evidence/api/UT-09-006.json) |
| UT-09-007 | `POST /api/v1/reservations` | 異常 | 停止中の病院への申請 | SUSPENDEDの病院の予約枠 | 409 RESERVATION-009 | OK | [UT-09-007.json](../evidence/api/UT-09-007.json) |
| UT-09-008 | `GET /api/v1/reservations` | 正常 | 予約一覧 | 本人と他人がそれぞれ予約している | 200、本人の予約だけを病院名・ペット名・日時・状態付きで返す | OK | [UT-09-008.json](../evidence/api/UT-09-008.json) |
| UT-09-009 | `GET /api/v1/reservations/{reservationId}` | 正常 | 予約詳細 | 本人の予約 | 200、メモを含む | OK | [UT-09-009.json](../evidence/api/UT-09-009.json) |
| UT-09-010 | `GET /api/v1/reservations/{reservationId}` | 異常 | 他人の予約・存在しない予約 | 他のユーザーの予約ID／存在しない予約ID | 403 RESERVATION-007／404 RESERVATION-006 | OK | [UT-09-010.json](../evidence/api/UT-09-010.json) |
| UT-09-011 | `PATCH /api/v1/reservations/{reservationId}/cancel` | 正常 | 予約キャンセル | 申請中の本人の予約 | 200、CANCELLEDになり、同じ予約枠に別の予約を申請できる | OK | [UT-09-011.json](../evidence/api/UT-09-011.json) |
| UT-09-012 | `PATCH /api/v1/reservations/{reservationId}/cancel` | 異常 | 不正な状態遷移・他人の予約 | キャンセル済みの予約／他のユーザーの予約 | 409 RESERVATION-008／403 RESERVATION-007 | OK | [UT-09-012.json](../evidence/api/UT-09-012.json) |
| UT-09-013 | `PATCH /api/v1/reservations/{reservationId}/cancel` | 異常 | 存在しない予約のキャンセル（Codex G-05） | 存在しない予約ID | 404 RESERVATION-006 | OK | [UT-09-013.json](../evidence/api/UT-09-013.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | BLOCKED状態の予約枠への申請時、RESERVATION-003例外が発生する | 合格 |
| 2 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | RSV-T001: 正常な予約申請時、REQUESTED状態の予約を作成する | 合格 |
| 3 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | RSV-T002: 同一予約枠への重複予約時、RESERVATION-001例外が発生する | 合格 |
| 4 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | RSV-T003: 他人のペットで予約申請時、RESERVATION-004例外が発生する | 合格 |
| 5 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 過去日時の予約枠への申請時、RESERVATION-005例外が発生する | 合格 |
| 6 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 指定した病院に属さない予約枠で申請時、RESERVATION-002例外が発生する | 合格 |
| 7 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 存在しないペットで予約申請時、RESERVATION-004例外が発生する | 合格 |
| 8 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 予約枠が存在しない場合、RESERVATION-002例外が発生する | 合格 |
| 9 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 予約枠は一致するが病院が取得できない場合、RESERVATION-009例外が発生する | 合格 |
| 10 | [CreateReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.CreateReservationServiceTest.xml) | 利用停止中の病院への申請時、RESERVATION-009例外が発生する | 合格 |
| 11 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | RSV-T006: 予約詳細取得成功時、詳細を返却する | 合格 |
| 12 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 存在しない予約の詳細取得時、RESERVATION-006例外が発生する | 合格 |
| 13 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 他人の予約詳細取得時、RESERVATION-007例外が発生する | 合格 |
| 14 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約一覧で病院が取得できない場合、RESERVATION-002例外が発生する | 合格 |
| 15 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約一覧で予約枠が取得できない場合、RESERVATION-002例外が発生する | 合格 |
| 16 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約一覧で論理削除済みを含めてもペットが取得できない場合、RESERVATION-004例外が発生する | 合格 |
| 17 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約一覧取得成功時、ユーザーの予約一覧を返却する | 合格 |
| 18 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約詳細で病院が取得できない場合、RESERVATION-002例外が発生する | 合格 |
| 19 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約詳細で予約枠が取得できない場合、RESERVATION-002例外が発生する | 合格 |
| 20 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 予約詳細で論理削除済みを含めてもペットが取得できない場合、RESERVATION-004例外が発生する | 合格 |
| 21 | [GetReservationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetReservationServiceTest.xml) | 論理削除済みペットの予約も一覧に表示される（他の予約も巻き込まれない） | 合格 |
| 22 | [ReservationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 23 | [ReservationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 24 | [ReservationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationPersistenceAdapterTest.xml) | 条件付き更新が0件の場合、falseを返却する（他の遷移が先に成立） | 合格 |
| 25 | [ReservationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationPersistenceAdapterTest.xml) | 条件付き更新が1件成立した場合、trueを返却する | 合格 |
| 26 | [ReservationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationPersistenceAdapterTest.xml) | 予約枠の一意制約に違反した場合、RESERVATION-001例外に変換する | 合格 |
| 27 | [ReservationPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationPersistenceAdapterTest.xml) | 予約枠の一意制約以外の整合性違反は変換せずそのまま伝播する | 合格 |
| 28 | [ReservationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationControllerTest.xml) | GET /api/v1/reservations - 予約一覧取得成功 | 合格 |
| 29 | [ReservationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationControllerTest.xml) | GET /api/v1/reservations/{reservationId} - 予約詳細取得成功 | 合格 |
| 30 | [ReservationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationControllerTest.xml) | PATCH /api/v1/reservations/{reservationId}/cancel - 予約キャンセル成功 | 合格 |
| 31 | [ReservationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationControllerTest.xml) | POST /api/v1/reservations - petIdがnullの場合Validation失敗 | 合格 |
| 32 | [ReservationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationControllerTest.xml) | POST /api/v1/reservations - 予約申請成功 | 合格 |
