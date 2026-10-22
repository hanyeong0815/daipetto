# 単体テスト仕様書・結果 UT-14 画面

| 項目 | 内容 |
| --- | --- |
| 対象機能 | 画面 SC-001〜SC-016（docs/05 画面設計書） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（UT-14-001〜022）、Sonnet 5.5 サブエージェント（UT-14-023〜033） |
| 結果 | 33/33 OK（不具合 D-007・D-009 を発見し、修正後の再テストで OK） |
| 環境 | `../README.md` の「実施環境」。画面は Vite（5173）→ 画面テスト用 Spring API（18082、DB `daipetto_ut_screen`）。データは `../scripts/seed-screen-data.mjs` で準備 |

## 前提データ

`seed-screen-data.mjs` が API 経由で登録する。

| 種類 | 内容 |
| --- | --- |
| ユーザー | 田中（一般）、佐藤（一般）、さくら院長（病院管理者・さくら動物病院に紐付け）、システム管理者 |
| 病院 | さくら動物病院（営業中・月〜土の診療時間・今月と来月の予約枠を自動生成）、みどり動物病院（営業中）、ひかり動物病院（停止中） |
| ペット（田中） | モモ（猫・スコティッシュフォールド）、ポチ（犬・柴）。モモの健康記録3件、ワクチン（3種混合: 予定、狂犬病: 接種済み） |
| 予約（田中） | さくら動物病院 10/26 09:00（予約確定・承認通知あり）、10/26 10:30（申請中） |

ログインは、UT-14-001〜004 で画面から行い、以降は同じ利用者のセッション（Refresh Token）をブラウザに設定して行った。管理者の各画面も同様。確認ダイアログ（`window.confirm`）はテスト中は自動でOKにし、表示された文言をエビデンスに記録した。

## テスト項目と結果

| 項目ID | 画面 | 操作者 | テスト観点・操作 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- |
| UT-14-001 | SC-001 ログイン | 未ログイン | 初期表示 | メールアドレス・パスワード・ログインボタン・会員登録リンクが表示される | OK | [画面](../evidence/screen/UT-14-001_SC-001_login-initial.jpg) |
| UT-14-002 | SC-001 ログイン | 未ログイン | 空欄のままログイン | 必須入力チェックで送信されない（ログインAPIを呼ばない） | OK | [確認結果](../evidence/screen/UT-14-002_SC-001_required.json) |
| UT-14-003 | SC-001 ログイン | 未ログイン | 誤ったパスワードでログイン | 「メールアドレスまたはパスワードが正しくありません。」を表示し、ログイン画面のまま（API 401） | OK | [画面](../evidence/screen/UT-14-003_SC-001_login-wrong-password.jpg) |
| UT-14-004 | SC-001 → SC-003 | 田中 | 正しいパスワードでログイン | ダッシュボードへ遷移し、次の予約（10/26 09:00 予約確定）・マイペット・健康サマリーを表示 | OK | [画面](../evidence/screen/UT-14-004_SC-003_login-success-dashboard.jpg) |
| UT-14-005 | SC-004 マイペット | 田中 | 一覧表示 | 本人のペット（種類・品種・体重）を表示 | OK | [画面](../evidence/screen/UT-14-005_SC-004_pet-list.jpg) |
| UT-14-006 | SC-006 ペット詳細 | 田中 | モモの詳細 | 品種・性別・不妊手術・生年月日・体重・マイクロチップ・ワクチン予定（あと5日）を表示 | OK | [画面](../evidence/screen/UT-14-006_SC-006_pet-detail.jpg) |
| UT-14-007 | SC-005 ペット登録 | 田中 | 名前・品種・生年月日・性別（メス）・体重を入力 | 入力内容が画面に反映される | OK | [画面](../evidence/screen/UT-14-007_SC-005_pet-register-input.jpg) |
| UT-14-008 | SC-005 ペット登録 | 田中 | 「登録する」 | 「登録完了」の後マイペットへ戻り、ハナが追加される。DBに入力どおり保存 | OK | [画面](../evidence/screen/UT-14-008_SC-005_pet-register-result.jpg)・[DB](../evidence/screen/UT-14-008_SC-005_pet-register-db.txt) |
| UT-14-009 | SC-005 ペット編集 | 田中 | ハナの編集画面を開く | 種類・名前・品種・生年月日・性別が登録済みの値で表示される | OK | [画面](../evidence/screen/UT-14-009_SC-005_pet-edit-prefilled.jpg) |
| UT-14-010 | SC-005 ペット編集 | 田中 | 体重を 3.4 に変えて保存 | 詳細画面へ戻り、体重 3.4 kg と表示 | OK | [画面](../evidence/screen/UT-14-010_SC-005_pet-edit-saved.jpg) |
| UT-14-011 | SC-007 健康記録 | 田中 | モモの健康記録一覧 | 記録日の降順で体重・症状・メモを表示 | OK | [画面](../evidence/screen/UT-14-011_SC-007_health-record-list.jpg) |
| UT-14-012 | SC-007 健康記録 | 田中 | 体重・症状・メモを入力して追加 | 当日（JST）の記録が先頭に追加される | OK | [画面](../evidence/screen/UT-14-012_SC-007_health-record-added.jpg) |
| UT-14-013 | SC-016 ワクチン予定 | 田中 | 一覧表示 | 接種済み（接種日）と予定（あと5日・接種完了/中止/削除ボタン）を表示 | OK | [画面](../evidence/screen/UT-14-013_SC-016_vaccination-list.jpg) |
| UT-14-014 | SC-016 ワクチン予定 | 田中 | 3種混合の「接種完了」→ 接種日（当日）で確定 | 接種済みになり、接種日が表示される | OK | [入力](../evidence/screen/UT-14-014_SC-016_vaccination-complete-input.jpg)・[結果](../evidence/screen/UT-14-014_SC-016_vaccination-complete-result.jpg) |
| UT-14-015 | SC-008 病院を探す | 田中 | 一覧表示 | 営業中の2病院だけを表示（停止中のひかり動物病院は出ない） | OK | [画面](../evidence/screen/UT-14-015_SC-008_hospital-list.jpg) |
| UT-14-016 | SC-008 病院を探す | 田中 | 地域「新宿」で検索 | みどり動物病院（新宿区）だけを表示。**初回は0件（D-007）→ 修正後に再テスト** | OK（修正後） | [修正前](../evidence/screen/UT-14-016_SC-008_area-search-before-fix-D-007.jpg)・[修正後](../evidence/screen/UT-14-016_SC-008_area-search.jpg) |
| UT-14-017 | SC-009 病院詳細 | 田中 | さくら動物病院の詳細 | 曜日別の診療時間（休憩を含む）、日曜は休診、予約ボタンを表示 | OK | [画面](../evidence/screen/UT-14-017_SC-009_hospital-detail.jpg) |
| UT-14-018 | SC-010 予約 | 田中 | ポチ・予防接種 → 日付・時間 → 確認 → 申請 | 日付は診療日だけ（日曜なし）、休憩時間（12:00・12:30）は選択不可。確認画面に病院・ペット・目的・日時を表示し、申請後は予約履歴へ | OK | [日時選択](../evidence/screen/UT-14-018_SC-010_reserve-datetime.jpg)・[確認](../evidence/screen/UT-14-018_SC-010_reserve-confirm.jpg) |
| UT-14-019 | SC-011 予約履歴 | 田中 | 申請後の一覧 | 申請した予約（10/28 10:30 ポチ）が「申請中」で表示される | OK | [画面](../evidence/screen/UT-14-019_SC-011_reservation-history.jpg) |
| UT-14-020 | SC-011 予約履歴 | 田中 | 申請した予約を「予約キャンセル」 | 確認ダイアログの後「キャンセル」になり、キャンセルボタンが消える | OK | [画面](../evidence/screen/UT-14-020_SC-011_reservation-cancel.jpg) |
| UT-14-021 | SC-012 通知 | 田中 | 未読の承認通知を開く | 未読件数の表示が消え、既読になる | OK | [未読](../evidence/screen/UT-14-021_SC-012_notification-unread.jpg)・[既読](../evidence/screen/UT-14-021_SC-012_notification-read.jpg) |
| UT-14-022 | 共通ヘッダー | 田中 | ログアウト | 確認の後 `POST /auth/logout` 200、ログイン画面へ遷移し、保存していたセッションが消える | OK | [画面](../evidence/screen/UT-14-022_SC-003_logout.jpg)・[確認結果](../evidence/screen/UT-14-022_SC-003_logout.json) |
| UT-14-023 | SC-002 会員登録 | 未ログイン | 短すぎるパスワード（6文字）で登録 | 登録されず、理由（パスワードの文字数）が表示される。**初回は理由の分からない固定文言だった（D-009）→ 修正後に再テスト** | OK（修正後） | [修正前](../evidence/screen/UT-14-023_SC-002_register-short-password_before-fix-D-009.jpg)・[修正後](../evidence/screen/UT-14-023_SC-002_register-short-password_after-fix.jpg)・[確認結果](../evidence/screen/UT-14-023_SC-002_register-short-password_after-fix.json)・[DB](../evidence/screen/UT-14-023_SC-002_register-short-password_after-fix-db.txt) |
| UT-14-024 | SC-002 会員登録 | 未ログイン | 新しいメールアドレス・ニックネーム山田・正しいパスワードで登録 | API 201、ログイン画面へ遷移。DBに一般ユーザー・利用中で登録 | OK | [入力](../evidence/screen/UT-14-024_SC-002_register-input.jpg)・[結果](../evidence/screen/UT-14-024_SC-002_register-result.jpg)・[DB](../evidence/screen/UT-14-024_SC-002_register-result-db.txt) |
| UT-14-025 | 権限ガード | 田中 | 管理画面のURL（/admin/users・/admin/reservations）を直接開く | ダッシュボードへ戻され、管理画面は表示されない | OK | [確認結果](../evidence/screen/UT-14-025_guard_role-guard.json)・[画面1](../evidence/screen/UT-14-025_guard_admin-users-redirect.jpg)・[画面2](../evidence/screen/UT-14-025_guard_admin-reservations-redirect.jpg) |
| UT-14-026 | SC-003 管理メニュー | さくら院長・システム管理者 | ダッシュボードの管理メニュー | 病院管理者は予約管理・病院情報管理の2つ、システム管理者はユーザー管理を含む3つ | OK | [病院管理者](../evidence/screen/UT-14-026_SC-003_admin-menu-hospital-admin.jpg)・[システム管理者](../evidence/screen/UT-14-026_SC-003_admin-menu-system-admin.jpg) |
| UT-14-027 | SC-013 予約管理 | さくら院長 | 一覧表示 | 病院名、状態別件数（承認待ち1・予約確定1・診療完了0）、予約者・ペット・日時・メモのカード | OK | [画面](../evidence/screen/UT-14-027_SC-013_admin-reservation-list.jpg)・[カード](../evidence/screen/UT-14-027_SC-013_admin-reservation-cards.jpg) |
| UT-14-028 | SC-013 予約管理 | さくら院長 | 申請中の予約を「承認」 | 確認文言に日時・予約者・ペット。予約確定になり「診療完了にする」が出る。DB: APPROVED、田中への承認通知 | OK | [前](../evidence/screen/UT-14-028_SC-013_approve-before.jpg)・[後](../evidence/screen/UT-14-028_SC-013_approve-after.jpg)・[DB](../evidence/screen/UT-14-028_SC-013_approve-db.txt) |
| UT-14-029 | SC-013 予約管理 | さくら院長 | 状態「予約確定」・日付 2026-10-26 で絞り込み | 条件に合う予約だけを表示（日付は `date` パラメータでAPIへ） | OK | [状態](../evidence/screen/UT-14-029_SC-013_filter-status-chip.jpg)・[日付](../evidence/screen/UT-14-029_SC-013_filter-date.jpg) |
| UT-14-030 | SC-014 病院管理 | さくら院長 | 画面表示と予約枠の自動生成（2か月先） | 紐付いた病院が自動選択、診療時間・自動生成欄を表示。生成で作成件数のメッセージ、DBに枠が作られる | OK | [初期](../evidence/screen/UT-14-030_SC-014_hospital-admin-initial.jpg)・[入力](../evidence/screen/UT-14-030_SC-014_slot-generate-input.jpg)・[メッセージ](../evidence/screen/UT-14-030_SC-014_slot-generate-success-message.jpg)・[DB](../evidence/screen/UT-14-030_SC-014_slot-generate-db.txt) |
| UT-14-031 | SC-014 病院管理 | システム管理者 | 病院の選択肢と病院管理者欄 | 停止中の病院に「（停止中）」。さくら動物病院を選ぶと病院管理者にさくら院長が表示される | OK | [初期](../evidence/screen/UT-14-031_SC-014_system-admin-initial.jpg)・[選択後](../evidence/screen/UT-14-031_SC-014_system-admin-selected.jpg)・[病院管理者](../evidence/screen/UT-14-031_SC-014_system-admin-hospital-admins.jpg) |
| UT-14-032 | SC-015 ユーザー管理 | システム管理者 | 「sato」で検索 → 佐藤を「アカウント停止」 | 1名表示。確認の後「停止中」「停止解除」表示。DB: SUSPENDED | OK | [検索](../evidence/screen/UT-14-032_SC-015_user-search-sato.jpg)・[停止後](../evidence/screen/UT-14-032_SC-015_user-suspended.jpg)・[DB](../evidence/screen/UT-14-032_SC-015_user-suspend-db.txt) |
| UT-14-033 | SC-002 会員登録 | 未ログイン | 登録済みのメールアドレスで登録（D-009 の確認として追加） | 「既に登録済みのメールアドレスです。」を表示し、登録されない（API 409） | OK | [画面](../evidence/screen/UT-14-033_SC-002_register-duplicate-email.jpg)・[確認結果](../evidence/screen/UT-14-033_SC-002_register-duplicate-email.json)・[DB](../evidence/screen/UT-14-033_SC-002_register-duplicate-email-db.txt) |

確認ダイアログの文言・ネットワーク要求・DB確認など画面に写らない内容は、各項目の `.json`・`.txt` に記録した。

## 補足

- 確認ダイアログはブラウザ標準の `confirm` のため、テストでは自動でOKを返すよう差し替え、表示された文言を記録した（例: UT-14-028「2026-10-26 10:30 田中様（モモ）の予約を承認しますか？」）。
- 選択欄（`<select>`）の一覧はブラウザ標準の表示で画面写しに写らないため、DOM から読み取って JSON に記録した（UT-14-031）。
- 保存完了などの一時メッセージ（約2秒で消える）は、確認のため一時的に表示時間を延ばした状態で写した（UT-14-030。製品コードは変更していない）。
