# 単体テスト仕様書・結果 UT-01 会員・認証

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 会員・認証（docs/07 §3・§4） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 16/16 OK、自動テスト（JUnit）41/41 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-01-001 | `POST /api/v1/users` | 正常 | 会員登録 | 未登録のメールアドレス・8文字以上のパスワード・ニックネーム | 201、userIdを返す。権限ROLE_USER・状態ACTIVEで登録され、パスワードはハッシュで保存される | OK | [UT-01-001.json](../evidence/api/UT-01-001.json) |
| UT-01-002 | `POST /api/v1/users` | 異常 | メールアドレスの重複 | 登録済みのメールアドレス | 409 USER-001 | OK | [UT-01-002.json](../evidence/api/UT-01-002.json) |
| UT-01-003 | `POST /api/v1/users` | 異常 | パスワードの長さ不足 | 7文字のパスワード | 400 VALIDATION-001（docs/07 §4-1） | OK | [UT-01-003.json](../evidence/api/UT-01-003.json) |
| UT-01-004 | `POST /api/v1/users` | 異常 | メールアドレスの形式 | 「@」の無いメールアドレス | 400 VALIDATION-001（docs/07 §4-1） | OK | [UT-01-004.json](../evidence/api/UT-01-004.json) |
| UT-01-005 | `POST /api/v1/auth/login` | 正常 | ログイン | 登録済みのメールアドレスと正しいパスワード | 200、Access Token・Refresh Tokenを返し、有効なRefresh TokenがDBに1件だけ残る | OK | [UT-01-005.json](../evidence/api/UT-01-005.json) |
| UT-01-006 | `POST /api/v1/auth/login` | 異常 | パスワードの誤り | 登録済みのメールアドレスと誤ったパスワード | 401 AUTH-001 | OK | [UT-01-006.json](../evidence/api/UT-01-006.json) |
| UT-01-007 | `POST /api/v1/auth/login` | 異常 | 未登録のメールアドレス | 登録されていないメールアドレス | 401 AUTH-001（存在の有無を区別しない） | OK | [UT-01-007.json](../evidence/api/UT-01-007.json) |
| UT-01-008 | `POST /api/v1/auth/login` | 異常 | 利用停止中のアカウント | 状態SUSPENDEDのユーザー | 403 AUTH-002、Tokenを発行しない | OK | [UT-01-008.json](../evidence/api/UT-01-008.json) |
| UT-01-009 | `GET /api/v1/users/me` | 正常 | マイページ取得 | ログイン中の一般ユーザー | 200、userId・email・nickname・role・statusを返し、パスワードを含まない | OK | [UT-01-009.json](../evidence/api/UT-01-009.json) |
| UT-01-010 | `POST /api/v1/auth/refresh` | 正常 | Token再発行（ローテーション） | 有効なRefresh Token | 200、新しいAccess Token・Refresh Tokenを返し、古いRefresh Tokenは失効する | OK | [UT-01-010.json](../evidence/api/UT-01-010.json) |
| UT-01-011 | `POST /api/v1/auth/refresh` | 異常 | 無効なRefresh Token | 形式の正しくない文字列 | 401 AUTH-003 | OK | [UT-01-011.json](../evidence/api/UT-01-011.json) |
| UT-01-012 | `POST /api/v1/auth/refresh` | 異常 | 利用停止中ユーザーの再発行 | 有効なRefresh Tokenを持つユーザーをDBで利用停止にした状態 | 403 AUTH-002（docs/07 §3-2） | OK | [UT-01-012.json](../evidence/api/UT-01-012.json) |
| UT-01-013 | `POST /api/v1/auth/logout` | 正常 | ログアウト | ログイン中のユーザー（Authorization: Bearer <Access Token>、Request本文なし） | 200、data.isLogout=true。そのユーザーの有効なRefresh Tokenがすべて失効し、以後の再発行はAUTH-003（docs/07 §3-3） | OK | [UT-01-013.json](../evidence/api/UT-01-013.json) |
| UT-01-014 | `GET /api/v1/users/me` | 異常 | Refresh TokenをAccess Tokenとして使用 | Authorization: Bearer <Refresh Token> | 401 AUTH-001（Token種別の取り違えを認証しない） | OK | [UT-01-014.json](../evidence/api/UT-01-014.json) |
| UT-01-015 | `GET /api/v1/users/me` | 異常 | 改ざんしたAccess Token | 署名部分を書き換えたAccess Token | 401 AUTH-001 | OK | [UT-01-015.json](../evidence/api/UT-01-015.json) |
| UT-01-016 | `POST /api/v1/auth/logout` | 異常 | 未認証でのログアウト | Authorizationヘッダーなし | 401 AUTH-001（docs/07 §3-3） | OK | [UT-01-016.json](../evidence/api/UT-01-016.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [GetMyProfileServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.GetMyProfileServiceTest.xml) | ユーザーIDで自分のプロフィールを取得する | 合格 |
| 2 | [GetMyProfileServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.GetMyProfileServiceTest.xml) | 存在しないユーザーIDの場合、USER-003例外が発生する | 合格 |
| 3 | [LoginServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LoginServiceTest.xml) | パスワードが一致しない場合、AUTH-001例外が発生する | 合格 |
| 4 | [LoginServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LoginServiceTest.xml) | ログイン成功時、Access Token・Refresh Token・Roleを返却する | 合格 |
| 5 | [LoginServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LoginServiceTest.xml) | 最初の読み取り後に停止が確定した場合、排他取得後の再確認でAUTH-002となりトークンを発行しない（REVIEW-001 R-01） | 合格 |
| 6 | [LoginServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LoginServiceTest.xml) | 存在しないメールアドレスの場合、AUTH-001例外が発生する | 合格 |
| 7 | [LoginServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LoginServiceTest.xml) | 停止中のユーザーはログインできない | 合格 |
| 8 | [LogoutServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LogoutServiceTest.xml) | ログアウト時、ユーザーの有効なRefresh Tokenをすべて失効させる | 合格 |
| 9 | [LogoutServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.LogoutServiceTest.xml) | 一括revokeより先にユーザー行を排他取得する（同時refreshが発行する後続Tokenの取りこぼし防止） | 合格 |
| 10 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 期限切れRefresh Tokenの場合、AUTH-004例外が発生する | 合格 |
| 11 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 失効済みRefresh Tokenの場合、AUTH-003例外が発生する | 合格 |
| 12 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 存在しないRefresh Tokenの場合、AUTH-003例外が発生する | 合格 |
| 13 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 停止中ユーザーの場合、AUTH-002例外が発生する | 合格 |
| 14 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 同時リフレッシュで既に消費されていた場合、失効0件となりAUTH-003例外が発生する | 合格 |
| 15 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 有効なRefresh Tokenだがユーザーが存在しない場合、AUTH-001例外が発生し後続Tokenは発行されない | 合格 |
| 16 | [RefreshTokenServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.auth.service.RefreshTokenServiceTest.xml) | 有効なRefresh Tokenの場合、新しいAccess TokenとRefresh Tokenを返却する | 合格 |
| 17 | [RefreshTokenMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenMapperTest.xml) | RefreshToken DomainをEntityに変換する | 合格 |
| 18 | [RefreshTokenMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenMapperTest.xml) | RefreshTokenEntityをDomainに変換する | 合格 |
| 19 | [RefreshTokenPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenPersistenceAdapterTest.xml) | Refresh Tokenを保存する | 合格 |
| 20 | [RefreshTokenPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenPersistenceAdapterTest.xml) | ユーザーの有効なRefresh Tokenをすべて失効状態に更新する | 合格 |
| 21 | [RefreshTokenPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenPersistenceAdapterTest.xml) | 既に失効済みのRefresh Tokenを再度失効させると0件を返却する（rotationの単一消費） | 合格 |
| 22 | [RefreshTokenPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.auth.RefreshTokenPersistenceAdapterTest.xml) | 指定したRefresh Tokenのみ失効状態に更新する | 合格 |
| 23 | [JwtAuthenticationFilterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtAuthenticationFilterTest.xml) | Access Tokenの場合、userIdとroleで認証情報を設定する | 合格 |
| 24 | [JwtAuthenticationFilterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtAuthenticationFilterTest.xml) | Refresh TokenをBearerに渡した場合、例外を投げず未認証のまま次のフィルターへ進む | 合格 |
| 25 | [JwtAuthenticationFilterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtAuthenticationFilterTest.xml) | 権限はトークン内の値ではなく現在の値で設定する（権限変更が次のリクエストから反映される） | 合格 |
| 26 | [JwtAuthenticationFilterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtAuthenticationFilterTest.xml) | 停止中・論理削除済み・存在しないユーザーは、有効なAccess Tokenでも認証しない（08_State_Design §3-4） | 合格 |
| 27 | [JwtAuthenticationFilterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtAuthenticationFilterTest.xml) | 不正なTokenの場合、例外を投げず未認証のまま次のフィルターへ進む | 合格 |
| 28 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | Access TokenはvalidateAccessTokenでtrueを返却する | 合格 |
| 29 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | Access Tokenを生成し、userIdとroleを取得できる | 合格 |
| 30 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | Access Tokenを生成し、userIdを取得できる | 合格 |
| 31 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | Refresh TokenはvalidateAccessTokenでfalseを返却する | 合格 |
| 32 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | 同一ユーザー・同一秒に連続発行してもRefresh Tokenは重複しない | 合格 |
| 33 | [JwtProviderTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.security.JwtProviderTest.xml) | 不正なTokenの場合、validateTokenはfalseを返却する | 合格 |
| 34 | [AuthControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.auth.AuthControllerTest.xml) | POST /api/v1/auth/login - Validation失敗 | 合格 |
| 35 | [AuthControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.auth.AuthControllerTest.xml) | POST /api/v1/auth/login - ログイン成功 | 合格 |
| 36 | [AuthControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.auth.AuthControllerTest.xml) | POST /api/v1/auth/logout - 成功 | 合格 |
| 37 | [AuthControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.auth.AuthControllerTest.xml) | POST /api/v1/auth/refresh - Token再発行成功 | 合格 |
| 38 | [AuthControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.auth.AuthControllerTest.xml) | POST /api/v1/auth/refresh - Validation失敗 | 合格 |
| 39 | [UserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.UserControllerTest.xml) | GET /api/v1/users/me - 自分のプロフィール取得成功 | 合格 |
| 40 | [UserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.UserControllerTest.xml) | POST /api/v1/users - Validation失敗 | 合格 |
| 41 | [UserControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.user.UserControllerTest.xml) | POST /api/v1/users - 会員登録成功 | 合格 |
