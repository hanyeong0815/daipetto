# 単体テスト仕様書・結果 UT-10 予約管理（病院管理者）

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 予約管理（病院管理者）（docs/07 §9-5〜9-8） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 9/9 OK、自動テスト（JUnit）30/30 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-10-001 | `GET /api/v1/admin/reservations` | 正常 | 病院の予約一覧 | 紐付いた病院1の予約 | 200、予約者のニックネーム・ペット名・メモ・createdAt（+09:00）を含む | OK | [UT-10-001.json](../evidence/api/UT-10-001.json) |
| UT-10-002 | `GET /api/v1/admin/reservations` | 正常 | 状態・日付での絞り込み | 状態REQUESTED・予約枠の日付を指定 | 200、条件に一致する予約だけを日付・開始時刻順で返す | OK | [UT-10-002.json](../evidence/api/UT-10-002.json) |
| UT-10-003 | `PATCH /api/v1/admin/reservations/{reservationId}/approve` | 正常 | 予約承認と通知 | 申請中の予約（紐付いた病院管理者） | 200、APPROVEDになり、予約者にRESERVATION_APPROVEDの通知が1件作られる | OK | [UT-10-003.json](../evidence/api/UT-10-003.json) |
| UT-10-004 | `PATCH /api/v1/admin/reservations/{reservationId}/reject` | 正常 | 予約却下と通知 | 申請中の予約 | 200、REJECTEDになり、予約者にRESERVATION_REJECTEDの通知が1件だけ作られる | OK | [UT-10-004.json](../evidence/api/UT-10-004.json) |
| UT-10-005 | `PATCH /api/v1/admin/reservations/{reservationId}/complete` | 正常 | 診療完了と通知 | 承認済みの予約 | 200、COMPLETEDになり、予約者にTREATMENT_COMPLETEDの通知が作られる（承認時の通知と合わせて2件、docs/07 §12） | OK | [UT-10-005.json](../evidence/api/UT-10-005.json) |
| UT-10-006 | `PATCH .../approve, /complete` | 異常 | 不正な状態遷移 | 申請中の予約を診療完了／承認済みの予約を再承認 | いずれも409 RESERVATION-008、通知は作られない | OK | [UT-10-006.json](../evidence/api/UT-10-006.json) |
| UT-10-007 | `PATCH /api/v1/admin/reservations/{reservationId}/approve` | 異常 | 存在しない予約・紐付いていない病院 | 存在しない予約ID／病院2の予約を病院1の管理者が承認 | 404 RESERVATION-006／403 HOSPITAL-005（状態は変わらない） | OK | [UT-10-007.json](../evidence/api/UT-10-007.json) |
| UT-10-008 | `GET /api/v1/admin/reservations` | 異常 | 権限・入力チェック | 一般ユーザー／hospitalIdなし／status=FOO／紐付いていない病院 | 403 AUTH-005／400 VALIDATION-001／400 VALIDATION-001／403 HOSPITAL-005 | OK | [UT-10-008.json](../evidence/api/UT-10-008.json) |
| UT-10-009 | `PATCH /api/v1/admin/reservations/{reservationId}/complete` | 異常 | 存在しない予約の診療完了（Codex G-05） | 存在しない予約ID（病院管理者） | 404 RESERVATION-006（予約の確認が所属の確認より先）、通知は作られない | OK | [UT-10-009.json](../evidence/api/UT-10-009.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [GetAdminReservationListServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetAdminReservationListServiceTest.xml) | 管理対象の確認後に、病院・状態・日付で一覧を取得して応答に変換する | 合格 |
| 2 | [GetAdminReservationListServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.GetAdminReservationListServiceTest.xml) | 管理対象外の病院はHOSPITAL-005となり、一覧を取得しない | 合格 |
| 3 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | APPROVED状態の予約却下時、RESERVATION-008例外が発生する | 合格 |
| 4 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | CANCELLED状態の予約却下時、RESERVATION-008例外が発生する | 合格 |
| 5 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | COMPLETED状態の予約却下時、RESERVATION-008例外が発生する | 合格 |
| 6 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | REJECTED状態の予約却下時、RESERVATION-008例外が発生する | 合格 |
| 7 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | REQUESTED状態の診療完了時、RESERVATION-008例外が発生する | 合格 |
| 8 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | REQUESTED状態の予約却下時、REJECTEDへ遷移する | 合格 |
| 9 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | RSV-T004・STATE-T003: REQUESTED状態の予約キャンセル時、CANCELLEDへ遷移する | 合格 |
| 10 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | RSV-T005: COMPLETED状態の予約キャンセル時、RESERVATION-008例外が発生する | 合格 |
| 11 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T001: REQUESTED状態の予約承認時、APPROVEDへ遷移する | 合格 |
| 12 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T004: APPROVED状態の診療完了時、COMPLETEDへ遷移する | 合格 |
| 13 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T005: APPROVED状態の予約キャンセル時、CANCELLEDへ遷移する | 合格 |
| 14 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T006: COMPLETED状態の予約承認時、RESERVATION-008例外が発生する | 合格 |
| 15 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T007: REJECTED状態の予約承認時、RESERVATION-008例外が発生する | 合格 |
| 16 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | STATE-T008: CANCELLED状態の予約承認時、RESERVATION-008例外が発生する | 合格 |
| 17 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | キャンセルの直前に診療完了が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する | 合格 |
| 18 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 管理対象外の病院の予約は承認・却下・完了できず（HOSPITAL-005）、状態も通知も変えない | 合格 |
| 19 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 却下の直前に承認が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する | 合格 |
| 20 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 承認の直前に他の遷移が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する | 合格 |
| 21 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 診療完了の直前にキャンセルが成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する | 合格 |
| 22 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 存在しない予約の却下時、RESERVATION-006例外が発生する | 合格 |
| 23 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 存在しない予約の承認時、RESERVATION-006例外が発生する | 合格 |
| 24 | [ReservationStateTransitionServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.reservation.service.ReservationStateTransitionServiceTest.xml) | 他人の予約キャンセル時、RESERVATION-007例外が発生する | 合格 |
| 25 | [ReservationAdminQueryTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.reservation.ReservationAdminQueryTest.xml) | 病院の予約を予約日・開始時刻順に、予約者・ペット付きで返す。状態・日付の絞り込み、null条件、論理削除の除外 | 合格 |
| 26 | [ReservationAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationAdminControllerTest.xml) | GET /api/v1/admin/reservations - hospitalIdなし・不正な日付は400、管理対象外は403（HOSPITAL-005） | 合格 |
| 27 | [ReservationAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationAdminControllerTest.xml) | GET /api/v1/admin/reservations - 病院・状態・日付を渡し、予約者・ペット付きの一覧を返す | 合格 |
| 28 | [ReservationAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationAdminControllerTest.xml) | PATCH /api/v1/admin/reservations/{reservationId}/approve - 予約承認成功 | 合格 |
| 29 | [ReservationAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationAdminControllerTest.xml) | PATCH /api/v1/admin/reservations/{reservationId}/complete - 診療完了成功 | 合格 |
| 30 | [ReservationAdminControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.reservation.ReservationAdminControllerTest.xml) | PATCH /api/v1/admin/reservations/{reservationId}/reject - 予約却下成功 | 合格 |
