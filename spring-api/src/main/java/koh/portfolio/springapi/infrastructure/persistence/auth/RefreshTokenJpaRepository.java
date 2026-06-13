package koh.portfolio.springapi.infrastructure.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
    // select area
    Optional<RefreshTokenEntity> findByToken(String token);

    // update area
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update RefreshTokenEntity set revoked = true where userId = ?1 and revoked = false"
    )
    int revokeAllActiveTokensByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update RefreshTokenEntity set revoked = true where token = ?1 and revoked = false"
    )
    void revokeByToken(String token);
}
