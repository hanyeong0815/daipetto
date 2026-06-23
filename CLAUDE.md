# Daipetto — Claude Code Shared Context

このファイルはVSCode・IntelliJ・Git Bash CLIのすべてのClaude Codeインスタンスで自動ロードされる共有コンテキストです。

## 作業開始前に必ず読むこと

**詳細な設計・制約・実装規則はすべてここにあります:**

```
claude_code_handoff_daipetto.md
```

セッション開始時に上記ファイルを最初に読んでから作業してください。

---

## 作業領域とIDE対応

| 領域 | IDE | 主な技術 |
|---|---|---|
| `frontend/` | VSCode | React / TypeScript / Vite / Tailwind CSS |
| `django-api/` | VSCode | Python / Django REST Framework |
| `spring-api/` | IntelliJ | Java 17 / Spring Boot 3 / Spring Security |
| `docs/` | 共通 | 設計書（変更管理対象） |

---

## 言語規則

| 対象 | 言語 |
|---|---|
| 説明・回答 | 韓国語 |
| ドキュメント | 日本語 |
| コード識別子 | 英語 |
| テストメソッド名 | 英語 |
| @DisplayName | 日本語 |
| APIエラーメッセージ | 日本語 |

---

## 変更管理ルール

以下を変更した場合は、関連ドキュメント(`docs/`)も同時に更新すること。

- アーキテクチャ決定
- ERD / テーブル / カラム
- API endpoint / Request / Response
- Error code
- Role / Status値
- JWT / Refresh Token挙動
- Docker構成

---

## Spring API アーキテクチャ（変更禁止）

```
Controller → UseCase Interface → UseCase Service → Domain Model
→ Repository Port → Persistence Adapter → MapStruct Mapper
→ JPA Entity → JpaRepository → PostgreSQL
```

- Domain と JPA Entity は分離する
- UseCase は interface、Service は実装体
- Domain ↔ Entity 変換は MapStruct を使う
- Business error は ErrorCode + CustomException + Preconditions

---

## API共通仕様

**Success:**
```json
{ "success": true, "data": {} }
```

**Error:**
```json
{ "success": false, "data": null, "code": "AUTH-001", "message": "認証に失敗しました。" }
```

---

## 現在の実装状況

最新の進捗は `claude_code_handoff_daipetto.md` の Section 22 を参照。

完了済み: 会員登録・ログイン・JWT・Refresh Token・ログアウト・/users/me

次の作業候補: Pet API → Reservation API

---

## 作業後のルール

実装に変更があった場合:
1. `claude_code_handoff_daipetto.md` の Section 22（進捗）を更新する
2. 設計変更があれば該当 `docs/` ファイルも更新する
