package koh.portfolio.springapi.domain.user.port;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    boolean existsByEmail(String email);
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);

    // ログイン・再発行・ログアウトをユーザー単位で直列化するための排他取得。
    // これが無いと、refreshが旧Tokenを失効させて後続Tokenをinsertする間に
    // login/logoutの一括revoke（user_id条件）が走り、後続Tokenを取りこぼす
    void lockForSessionUpdate(Long userId);

    // 列の値をDBから直接読む（同じトランザクションで読み込み済みのUserはキャッシュの値を返すため、
    // 排他取得後の最新状態の確認にはこちらを使う）。存在しない場合は null
    UserStatus findStatusById(Long id);

    // 管理者向け検索。keyword はメールアドレス・ニックネームの部分一致（大文字小文字を区別しない）、
    // null の条件では絞り込まない。論理削除済みは除き、ID昇順で page（0始まり）・size 件を返す
    List<User> search(String keyword, Role role, UserStatus status, int page, int size);
    long count(String keyword, Role role, UserStatus status);

    // 状態・権限は対象の列だけを条件付きUPDATEで書く。論理削除済み・存在しない場合は false
    boolean updateStatus(Long id, UserStatus status, LocalDateTime updatedAt);
    boolean updateRole(Long id, Role role, LocalDateTime updatedAt);
}
