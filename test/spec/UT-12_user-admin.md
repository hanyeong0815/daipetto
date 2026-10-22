# 単体テスト仕様書・結果 UT-12 ユーザー管理（システム管理者）

| 項目 | 内容 |
| --- | --- |
| 対象機能 | ユーザー管理（システム管理者）（docs/07 §11-4〜11-7） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 7/7 OK、自動テスト（JUnit）20/20 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-12-001 | `GET /api/v1/admin/users` | 正常 | 検索（キーワード・権限・状態・ページ） | ニックネームの一部（大文字小文字違い）、role=ROLE_HOSPITAL_ADMIN、size=1 | 200、条件に一致するユーザーをID順・ページ形式で返し、パスワードを含まない | OK | [UT-12-001.json](../evidence/api/UT-12-001.json) |
| UT-12-002 | `PATCH /api/v1/admin/users/{userId}/suspend` | 正常 | アカウント停止 | ログイン中の一般ユーザー | 200、SUSPENDEDになり、Refresh Tokenがすべて失効し、発行済みのAccess Tokenでも次のリクエストは401 | OK | [UT-12-002.json](../evidence/api/UT-12-002.json) |
| UT-12-003 | `PATCH /api/v1/admin/users/{userId}/activate` | 正常 | 停止解除 | 停止中のユーザー | 200、ACTIVEに戻り、再ログインできる | OK | [UT-12-003.json](../evidence/api/UT-12-003.json) |
| UT-12-004 | `PATCH /api/v1/admin/users/{userId}/role` | 正常 | 権限変更の即時反映 | 一般ユーザーを病院管理者に変更 | 200、同じAccess Tokenの次のリクエストから新しい権限で認可される（管理API 403 → 200） | OK | [UT-12-004.json](../evidence/api/UT-12-004.json) |
| UT-12-005 | `PATCH /api/v1/admin/users/{userId}/suspend, /role` | 異常 | 自分自身・存在しないユーザー・不正な権限名 | 自分自身の停止／存在しないユーザー／role=ROLE_ROOT | 400 USER-005／404 USER-003／400 VALIDATION-001 | OK | [UT-12-005.json](../evidence/api/UT-12-005.json) |
| UT-12-006 | `GET /api/v1/admin/users` | 異常 | 権限（システム管理者以外） | 一般ユーザー／病院管理者 | いずれも403 AUTH-005 | OK | [UT-12-006.json](../evidence/api/UT-12-006.json) |
| UT-12-007 | `PATCH /api/v1/admin/users/{userId}/activate` | 異常 | 停止解除の自分自身・存在しないユーザー（Codex G-06） | 自分自身の停止解除／存在しないユーザーの停止解除 | 400 USER-005／404 USER-003 | OK | [UT-12-007.json](../evidence/api/UT-12-007.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | ユーザー検索結果をパスワードを含まない応答に変換し、ページ情報を付ける | 合格 |
| 2 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 権限変更は対象ユーザーの権限列だけを更新する | 合格 |
| 3 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 自分自身の権限変更はUSER-005例外、存在しないユーザーはUSER-003例外が発生する | 合格 |
| 4 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 自分自身の停止はUSER-005例外が発生し、何も更新しない | 合格 |
| 5 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 存在しない・論理削除済みユーザーの停止はUSER-003例外が発生し、トークンは失効させない | 合格 |
| 6 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 停止時はユーザー行を排他取得してから状態を更新し、Refresh Tokenをすべて失効させる | 合格 |
| 7 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 範囲外のページ指定は丸め、空白のキーワードは条件なしとして扱う | 合格 |
| 8 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 病院管理者以外の権限に変更すると、病院の紐付けをすべて外す | 合格 |
| 9 | [AdminUserServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.user.service.AdminUserServiceTest.xml) | 復旧時は状態だけを更新し、トークンは失効させない | 合格 |
| 10 | [UserMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.user.UserMapperTest.xml) | domainを_entityに_変換() | 合格 |
| 11 | [UserPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.user.UserPersistenceAdapterTest.xml) | セッション更新用のユーザー行排他取得が実DBで実行できる | 合格 |
| 12 | [UserPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.user.UserPersistenceAdapterTest.xml) | 管理者向け検索: メール・ニックネームの部分一致（大文字小文字無視）、権限・状態の絞り込み、論理削除の除外、ページング | 合格 |
| 13 | [UserPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.user.UserPersistenceAdapterTest.xml) | 状態・権限の更新は対象の列だけを書き、論理削除済み・存在しないユーザーは0件（false） | 合格 |
| 14 | [UserPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.user.UserPersistenceAdapterTest.xml) | 状態の読み直しは、同じ永続化コンテキストに古いUserが残っていてもDBの現在値を返す（REVIEW-001 R-01） | 合格 |
| 15 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | GET /api/v1/admin/users - 検索条件とページを渡し、ページ形式で返す | 合格 |
| 16 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | GET /api/v1/admin/users - 存在しない権限名は500ではなく400（VALIDATION-001） | 合格 |
| 17 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | PATCH /api/v1/admin/users/{id}/role - 権限なし・存在しない権限名は400（VALIDATION-001） | 合格 |
| 18 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | PATCH /api/v1/admin/users/{id}/role - 権限変更成功 | 合格 |
| 19 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | PATCH /api/v1/admin/users/{id}/suspend - 自分自身は400（USER-005） | 合格 |
| 20 | [AdminUserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.AdminUserControllerTest.xml) | PATCH /api/v1/admin/users/{id}/suspend・activate - 操作者IDと対象IDを渡す | 合格 |
