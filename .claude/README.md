# .claude/ — Claude Code 設定ディレクトリ

Daipetto プロジェクトの Claude Code 共有設定。VSCode / IntelliJ / CLI のすべてのインスタンスで共通に使う。

## 構成

```text
.claude/
├── claude.md                       詳細共有コンテキスト（Claude Code が自動ロード）
│                                   docs全13文書 + handoff の要約、未完成箇所調査、
│                                   アーキテクチャ・ErrorCode・状態遷移・進捗
├── agents/                         サブエージェント定義（Agent tool から呼び出し）
│   ├── spring-api-dev.md           Spring API 実装・修正・テスト
│   ├── frontend-dev.md             React 画面実装・API連動・状態管理
│   ├── django-api-dev.md           Django 分析API（未着手領域の構築から）
│   └── docs-sync.md                docs/handoff/TODO の同期更新・整合性監査
├── skills/                         スキル（/スキル名 で起動）
│   ├── docs-sync/SKILL.md          /docs-sync — 実装変更後の文書同期手順
│   ├── spring-new-domain/SKILL.md  /spring-new-domain — 新ドメイン追加手順（練習用ギャップ含む）
│   └── spring-test/SKILL.md        /spring-test — テスト作成規則と雛形
└── README.md                       このファイル
```

## 使い分け

| 状況 | 使うもの |
|---|---|
| Spring に新ドメイン追加（Reservation 等） | `/spring-new-domain` スキル（大規模なら spring-api-dev エージェント） |
| テスト作成・修正 | `/spring-test` スキル |
| 実装変更後の文書更新 | `/docs-sync` スキル（監査まで含むなら docs-sync エージェント） |
| Frontend の実API連動 | frontend-dev エージェント |
| Django 分析API 着手 | django-api-dev エージェント |

## メンテナンス規則

- 進捗・アーキテクチャ・ErrorCode に変化があったら `claude.md` の該当セクション（特に §2 未完成箇所・§4 ErrorCode・§6 Flyway・§9 進捗）を更新する
- ルート `CLAUDE.md` は軽量な入口に保ち、詳細はこの `claude.md` に集約する
- 練習用未実装項目（ユーザーが自分で実装する項目）の一覧は TODO.md が正。ここのファイルからは参照のみ
