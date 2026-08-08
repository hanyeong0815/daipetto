---
name: docs-sync
description: Daipetto の実装変更後に claude_code_handoff_daipetto.md §22・TODO.md・docs/ を同期更新する手順。API/DB/状態遷移/ErrorCode/環境の変更を文書に反映するとき、または「文書を更新して」「docs を同期して」と依頼されたときに使用する。
---

# docs-sync — 実装変更の文書同期手順

## いつ使うか

以下のいずれかを変更した直後（変更管理ルール — ルート CLAUDE.md 参照）:
アーキテクチャ決定 / ERD・テーブル・カラム / API endpoint・Request・Response /
ErrorCode / Role・Status値 / 状態遷移 / JWT・Refresh Token挙動 / Docker構成 / バージョン

## 手順

### Step 1 — 変更内容の棚卸し

今回のコード変更を列挙し、変更管理対象に該当するものを特定する。
文書ではなく**実コード**（Entity / Controller / ErrorCode enum / migration）を根拠にすること。

### Step 2 — handoff §22 更新

`claude_code_handoff_daipetto.md` の §22「現在 구현 진행 상황」に追記する。
- 完了項目は既存の記述スタイル（機能名 + 主要ファイル + 補足）に合わせる
- 「다음 추천 작업」リストの完了分を消し込み、順序を見直す

### Step 3 — TODO.md 更新

- 完了項目を `[x]` にし、必要なら実装メモ（実ファイル名・migration 番号）を添える
- **「あえて未実装のまま残した項目（練習用）」は完了扱いにしない**（ユーザーが自分で実装する項目）
- 新たに練習用ギャップを作った場合はこのセクションに追記する
- 冒頭の「最終更新」日付を更新する

### Step 4 — docs/ 更新

実ファイル名にはハッシュが付くため `docs/07_API_Design*.md` 形式で Glob 検索する。

| 変更 | 更新文書 |
|---|---|
| テーブル / カラム / Index | `docs/06_ERD*.md` |
| API / ErrorCode | `docs/07_API_Design*.md` |
| Status / 状態遷移 | `docs/08_State_Design*.md` |
| JWT / CORS / 認可 | `docs/09_Security_Design*.md` |
| バージョン / Docker | `docs/10_Development_Environment*.md` |
| テストシナリオ | `docs/11_Test_Strategy*.md` |
| 解決した障害 | `docs/12_Trouble_Shooting*.md` |
| アーキテクチャ | `docs/04_System_Architecture*.md` |

各文書の「ドキュメント情報」テーブルの Updated を今日の日付にする。
未実装機能を文書に載せる場合は見出しに「（未実装）」を付ける（07 の 11-2/11-3 の書式に合わせる）。

### Step 5 — 完了報告

更新したファイルと変更点を一覧で報告する。
実装と文書の食い違いを見つけたら、修正せずリストアップして報告する。

## 注意

- 既知の未完成箇所（`.claude/claude.md` §2）は今回の変更と無関係なら触らない
- 未マージブランチ（feat/spring/pet-fields）にのみ存在する文書更新と衝突しないよう、06/07 編集時は注意する
