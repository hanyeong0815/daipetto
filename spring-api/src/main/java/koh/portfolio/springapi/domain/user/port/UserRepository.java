package koh.portfolio.springapi.domain.user.port;

import koh.portfolio.springapi.domain.user.model.User;

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
}
