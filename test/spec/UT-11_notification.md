# 単体テスト仕様書・結果 UT-11 通知

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 通知（docs/07 §10） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 3/3 OK、自動テスト（JUnit）12/12 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-11-001 | `GET /api/v1/notifications` | 正常 | 通知一覧 | 承認・却下の通知を受け取った利用者と、通知を持つ他のユーザー | 200、本人宛の通知だけを作成日時の降順で返し、未読（isRead=false） | OK | [UT-11-001.json](../evidence/api/UT-11-001.json) |
| UT-11-002 | `PATCH /api/v1/notifications/{notificationId}/read` | 正常 | 既読化 | 本人宛の未読の通知 | 200、is_read=trueになる | OK | [UT-11-002.json](../evidence/api/UT-11-002.json) |
| UT-11-003 | `PATCH /api/v1/notifications/{notificationId}/read` | 異常 | 他人の通知・存在しない通知 | 他のユーザー宛の通知／存在しない通知ID | 403 NOTIFICATION-002（既読にならない）／404 NOTIFICATION-001 | OK | [UT-11-003.json](../evidence/api/UT-11-003.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | [1] type=RESERVATION_APPROVED, expectedTitle=予約が承認されました | 合格 |
| 2 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | [2] type=RESERVATION_REJECTED, expectedTitle=予約が却下されました | 合格 |
| 3 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | [3] type=TREATMENT_COMPLETED, expectedTitle=診療が完了しました | 合格 |
| 4 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | NOTI-T004: 通知一覧取得時、ユーザー別の通知を返却する | 合格 |
| 5 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | NOTI-T005: 通知既読処理時、is_readをtrueに更新する | 合格 |
| 6 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | 既読済みの通知を再度既読にしても例外にしない（冪等） | 合格 |
| 7 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | 存在しない通知の既読処理時、NOTIFICATION-001例外が発生する | 合格 |
| 8 | [NotificationServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.notification.service.NotificationServiceTest.xml) | 他ユーザーの通知の既読処理時、NOTIFICATION-002例外が発生する | 合格 |
| 9 | [NotificationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.notification.NotificationMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 10 | [NotificationMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.notification.NotificationMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 11 | [NotificationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.notification.NotificationControllerTest.xml) | GET /api/v1/notifications - 通知一覧取得成功 | 合格 |
| 12 | [NotificationControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.notification.NotificationControllerTest.xml) | PATCH /api/v1/notifications/{notificationId}/read - 通知既読処理成功 | 合格 |
