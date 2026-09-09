package koh.portfolio.springapi.domain.user.port;

import koh.portfolio.springapi.domain.user.model.User;

import java.util.Optional;

public interface UserRepository {
    boolean existsByEmail(String email);
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
}
