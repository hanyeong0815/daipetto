---
name: spring-new-domain
description: Daipetto の Spring API に新ドメイン（Reservation / HealthRecord / Vaccination / Notification 等）を追加する手順。ヘキサゴナル構造のファイル一式・Flyway migration・ErrorCode・練習用ギャップの残し方を含む。「新しいドメインを追加して」「〜APIを作って」と依頼されたときに使用する。
---

# spring-new-domain — 新ドメイン追加手順

## 事前確認（必須）

1. `docs/06_ERD*.md` でテーブル定義、`docs/07_API_Design*.md` で API 仕様、`docs/08_State_Design*.md` で状態遷移を確認する
2. 文書に仕様が無い場合は**設計追記が先**（docs-sync スキル参照）。設計せずに実装しない
3. Flyway 番号の衝突確認: `git branch -a` の全ブランチで `spring-api/src/main/resources/db/migration/` を確認する
   （V1〜V3, V5, V6 は現ブランチ、**V4 は未マージ feat/spring/pet-fields が使用済み**。V7 以降を使う）

## 作成ファイル一式（{domain} = 例: reservation）

アーキテクチャチェーン順に作成する。既存の Hospital / Pet 実装を必ず参考にすること。

```text
db/migration/V{n}__create_{domain}s.sql          テーブル（deleted_at 論理削除・created_at/updated_at）
domain/{domain}/model/{Domain}.java              @Getter @AllArgsConstructor のみ。JPA非依存。状態遷移ロジックはここ
domain/{domain}/exception/{Domain}ErrorCode.java enum。code() で "{DOMAIN}-{連番}" を返す。メッセージは日本語
domain/{domain}/port/{Domain}RepositoryPort.java
application/{domain}/dto/                        Create{Domain}Dto / {Domain}SummaryDto / {Domain}DetailDto 等
application/{domain}/usecase/                    UseCase interface（1操作1interface）
application/{domain}/service/                    実装体。単純ドメインは1 Service複数implements可、
                                                 複雑ドメイン（Reservation等）は UseCase別に Service分離
infrastructure/persistence/{domain}/{Domain}Entity.java
                                                 @Getter @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PRIVATE) @Builder
infrastructure/persistence/{domain}/{Domain}JpaRepository.java
infrastructure/persistence/{domain}/{Domain}PersistenceAdapter.java
infrastructure/mapper/ …                         @Mapper(componentModel="spring") + DomainEntityMapper<D, E> 継承
presentation/{domain}/{Domain}Controller.java    ApiResponse 返却
```

## 実装規則（抜粋 — 詳細は .claude/claude.md §3）

- `userId` は DTO に入れず `Authentication.getPrincipal()` から `Long` で取得
- owner チェックは `Preconditions.validate(...)` パターン（`UpdatePetService` 参照）
- Role 制限は `@PreAuthorize("hasAuthority('ROLE_...')")`（例: `HospitalController` の admin 系）
- 状態遷移は Domain Model 内のメソッドで制御し、禁止遷移は ErrorCode で弾く
- 重要な更新は `@Modifying` の明示的 update query
- 承認/却下/完了イベントでは Notification 生成（08_State_Design 6-6。Notification ドメイン実装後）

## 練習用ギャップ（ユーザーの学習方針 — 必須）

新ドメインの機能のうち **1〜2 個をあえて実装しない**。

1. 実装前にどの機能を残すかユーザーに提案・確認する（例: 状態変更系 1 本と登録系 1 本）
2. 残した項目を `TODO.md` の「あえて未実装のまま残した項目（練習用）」に、参考にすべき既存パターンと併せて記載する
3. `docs/07_API_Design*.md` の該当見出しに「（未実装）」を付ける

## 完了条件

1. `./gradlew clean test` が通る（spring-api/ で実行）
2. テスト作成は spring-test スキルの規則に従う
3. docs-sync スキルで handoff §22 → TODO.md → docs/ を更新する
