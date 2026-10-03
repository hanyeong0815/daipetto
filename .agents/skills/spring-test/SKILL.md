---
name: spring-test
description: Daipetto の Spring API テスト作成規則。Service / Controller / Mapper / PersistenceAdapter テストの書き方、@MockBean・WebMvcTest・Long principal の落とし穴を含む。「テストを書いて」「テストを直して」と依頼されたとき、または新規実装にテストを付けるときに使用する。
---

# spring-test — Spring API テスト作成手順

## 言語規則

```java
// クラス名・メソッド名: 英語 / @DisplayName: 日本語 / コメント: 最小限
@Test
@DisplayName("ログイン成功時、Access Token・Refresh Token・Roleを返却する")
void login_success() { }
```

## 環境の落とし穴（Spring Boot 3.3.13）

| やること | やらないこと | 理由 |
|---|---|---|
| `@MockBean` | `@MockitoBean` | Boot 3.3.13 には存在しない |
| `UsernamePasswordAuthenticationToken(1L, null, authorities)` | `.with(user("..."))` | principal は Long。String だと Controller の cast が失敗 |
| Controller の全 UseCase を `@MockBean` | 一部だけ Mock | 漏れると Context 生成失敗 |

## テスト種別ごとの雛形

### Service テスト（Unit）

- Mockito で Port / Mapper を Mock。`@ExtendWith(MockitoExtension.class)`
- 正常系 + ErrorCode 異常系（`CustomException` の `errorCode` を assert）
- 観点は `docs/11_Test_Strategy*.md` の Test ID（AUTH-T / PET-T / RSV-T / STATE-T 等）と対応させる

### Controller テスト（@WebMvcTest）

```java
@WebMvcTest(controllers = XxxController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))  // JwtProvider 依存で Context 失敗する場合
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
```

- `ApiResponse` 形式（`$.success` / `$.data` / `$.code` / `$.message`）で assert
- validation 失敗系も 1 本以上
- 認証が要る endpoint は `.principal(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))))`

### Mapper テスト

- Domain → Entity / Entity → Domain の双方向。`Mappers.getMapper(...)` で取得
- MapStruct 生成クラスが無ければ `./gradlew clean compileJava`

### PersistenceAdapter テスト

- `@Modifying` update query が対象行のみ更新することを検証（例: `revokeByToken`）

## 状態遷移テスト（Reservation 実装時）

`docs/11_Test_Strategy*.md` 6-7 の STATE-T001〜T008 を網羅する:
許可遷移 5 本（REQUESTED→APPROVED/REJECTED/CANCELLED, APPROVED→COMPLETED/CANCELLED）と
禁止遷移（COMPLETED→承認, REJECTED→承認, CANCELLED→承認）のエラーを両方テストする。

## 実行

```bash
cd spring-api && ./gradlew clean test
```

失敗が残る場合は原因を特定して報告する。テストを通すために本番コードを変える場合は、その旨と影響文書を必ず報告する。
