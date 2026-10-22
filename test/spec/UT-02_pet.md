# 単体テスト仕様書・結果 UT-02 ペット

| 項目 | 内容 |
| --- | --- |
| 対象機能 | ペット（docs/07 §5） |
| 実施日 | 2026-10-23 |
| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |
| 結果 | API単体テスト 13/13 OK、自動テスト（JUnit）28/28 合格 |
| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |

## 1. API単体テスト（実HTTP・実DB）

使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。

| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UT-02-001 | `POST /api/v1/pets` | 正常 | ペット登録（全項目） | 名前・種類・誕生日・性別・体重・品種・去勢/避妊・マイクロチップ番号 | 201、petIdを返し、全項目が保存される | OK | [UT-02-001.json](../evidence/api/UT-02-001.json) |
| UT-02-002 | `POST /api/v1/pets` | 正常 | 任意項目の省略 | 名前・種類だけ（性別・体重・品種などを省略） | 201、省略した項目はNULL（性別は「不明」）、去勢/避妊はfalse | OK | [UT-02-002.json](../evidence/api/UT-02-002.json) |
| UT-02-003 | `POST /api/v1/pets` | 異常 | 名前の必須・長さ | 名前なし／51文字の名前 | いずれも400 VALIDATION-001 | OK | [UT-02-003.json](../evidence/api/UT-02-003.json) |
| UT-02-004 | `POST /api/v1/pets` | 異常 | 体重の範囲・桁数 | 体重 -1／体重 1000（整数部4桁） | いずれも400 VALIDATION-001 | OK | [UT-02-004.json](../evidence/api/UT-02-004.json) |
| UT-02-005 | `POST /api/v1/pets` | 異常 | 種類（enum）の不正値 | petType = "DRAGON"（許容値 CAT/DOG/RABBIT/HAMSTER/BIRD/OTHER 以外） | 400 VALIDATION-001（500にしない） | OK | [UT-02-005.json](../evidence/api/UT-02-005.json) |
| UT-02-006 | `GET /api/v1/pets` | 正常 | ペット一覧 | 本人と他人がそれぞれペットを持つ | 200、本人のペットだけを返す（他人のペットを含まない） | OK | [UT-02-006.json](../evidence/api/UT-02-006.json) |
| UT-02-007 | `GET /api/v1/pets/{petId}` | 正常 | ペット詳細 | 本人のペット | 200、全項目とcreatedAt（+09:00）を返す | OK | [UT-02-007.json](../evidence/api/UT-02-007.json) |
| UT-02-008 | `GET /api/v1/pets/{petId}` | 異常 | 他人のペットの詳細 | 他のユーザーのペットID | 401 PET-002（飼い主が異なる） | OK | [UT-02-008.json](../evidence/api/UT-02-008.json) |
| UT-02-009 | `GET /api/v1/pets/{petId}` | 異常 | 存在しないペット | 存在しないペットID | 404 PET-001 | OK | [UT-02-009.json](../evidence/api/UT-02-009.json) |
| UT-02-010 | `PATCH /api/v1/pets/{petId}` | 正常 | ペット更新（全項目の置き換え） | 品種・体重ありのペットを、品種・体重を省略して更新 | 200、送った項目は更新され、省略した任意項目はNULLになる（docs/07 §5-4） | OK | [UT-02-010.json](../evidence/api/UT-02-010.json) |
| UT-02-011 | `PATCH /api/v1/pets/{petId}` | 異常 | 他人のペットの更新 | 他のユーザーのペットID | 401 PET-002、変更されない | OK | [UT-02-011.json](../evidence/api/UT-02-011.json) |
| UT-02-012 | `DELETE /api/v1/pets/{petId}` | 正常 | ペット削除（論理削除） | 本人のペット | 200、deleted_atが設定され、一覧に出ず、詳細は404 PET-001 | OK | [UT-02-012.json](../evidence/api/UT-02-012.json) |
| UT-02-013 | `DELETE /api/v1/pets/{petId}` | 異常 | 他人のペットの削除 | 他のユーザーのペットID | 401 PET-002、削除されない | OK | [UT-02-013.json](../evidence/api/UT-02-013.json) |

## 2. 自動テスト（JUnit：Service・Controller・Persistence）

クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。

| No | テストクラス | テスト内容 | 結果 |
| --- | --- | --- | --- |
| 1 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット一覧取得成功時、ユーザーのペット一覧を返却する | 合格 |
| 2 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット詳細取得成功時、ペット情報を返却する | 合格 |
| 3 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット情報更新時、breed・neutered・microchipNumberが更新される | 合格 |
| 4 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット情報更新成功時、プロフィール列だけを条件付きで更新する | 合格 |
| 5 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット登録時、breedとmicrochipNumberはnullでも登録できる | 合格 |
| 6 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット登録成功時、breed・neutered・microchipNumberがPetに正しく渡される | 合格 |
| 7 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット登録成功時、登録されたpetIdを返却する | 合格 |
| 8 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | ペット論理削除成功時、petRepository.softDeleteが呼ばれる | 合格 |
| 9 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 存在しないペットの詳細取得時、PET-001例外が発生する | 合格 |
| 10 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 存在しないペット更新時、PET-001例外が発生する | 合格 |
| 11 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 存在しないペット削除時、PET-001例外が発生する | 合格 |
| 12 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 他人のペット更新時、PET-002例外が発生する | 合格 |
| 13 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 他人のペット削除時、PET-002例外が発生する | 合格 |
| 14 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 他人のペット詳細取得時、PET-002例外が発生する | 合格 |
| 15 | [PetServiceTest](../evidence/junit/TEST-koh.portfolio.springapi.application.pet.service.PetServiceTest.xml) | 読み取り後にペットが論理削除されていた場合、更新は0件となりPET-001例外が発生する | 合格 |
| 16 | [PetMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.pet.PetMapperTest.xml) | DomainをEntityに変換できる | 合格 |
| 17 | [PetMapperTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.pet.PetMapperTest.xml) | EntityをDomainに変換できる | 合格 |
| 18 | [PetPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.pet.PetPersistenceAdapterTest.xml) | 論理削除されていないペットはプロフィール列が更新される | 合格 |
| 19 | [PetPersistenceAdapterTest](../evidence/junit/TEST-koh.portfolio.springapi.infrastructure.persistence.pet.PetPersistenceAdapterTest.xml) | 論理削除済みのペットは更新されず、削除状態が維持される | 合格 |
| 20 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | DELETE /api/v1/pets/{petId} - ペット論理削除成功 | 合格 |
| 21 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | GET /api/v1/pets - ペット一覧取得成功 | 合格 |
| 22 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | GET /api/v1/pets/{petId} - ペット詳細取得成功 | 合格 |
| 23 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | PATCH /api/v1/pets/{petId} - ペット情報更新成功 | 合格 |
| 24 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - nameが空白の場合Validation失敗 | 合格 |
| 25 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - petTypeがnullの場合Validation失敗 | 合格 |
| 26 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - ペット登録成功 | 合格 |
| 27 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - 性別は任意項目のため省略しても登録できる | 合格 |
| 28 | [PetControllerTest](../evidence/junit/TEST-koh.portfolio.springapi.presentation.pet.PetControllerTest.xml) | POST /api/v1/pets - 名前51文字・品種101文字・負の体重・桁数超過の体重はValidation失敗（列の長さ・DECIMAL(5,2)） | 合格 |
