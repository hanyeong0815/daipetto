package koh.portfolio.springapi.infrastructure.persistence.user;

import jakarta.persistence.LockModeType;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    // null の検索条件は CAST で型を明示する（PostgreSQLで lower(bytea) になるため、12_Trouble_Shooting §3-10）
    String SEARCH_CONDITION = " where u.deletedAt is null"
            + " and (:keyword is null"
            + " or lower(u.email) like lower(concat('%', cast(:keyword as string), '%'))"
            + " or lower(u.nickname) like lower(concat('%', cast(:keyword as string), '%')))"
            + " and (:role is null or u.role = :role)"
            + " and (:status is null or u.status = :status)";

    boolean existsByEmail(String email);
    Optional<UserEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select u.status from UserEntity u where u.id = :id")
    Optional<UserStatus> findStatusById(@Param("id") Long id);

    @Query("select u from UserEntity u" + SEARCH_CONDITION + " order by u.id")
    List<UserEntity> search(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query("select count(u) from UserEntity u" + SEARCH_CONDITION)
    long countBySearch(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("status") UserStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserEntity set status = :status, updatedAt = :updatedAt where id = :id and deletedAt is null")
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserEntity set role = :role, updatedAt = :updatedAt where id = :id and deletedAt is null")
    int updateRole(@Param("id") Long id, @Param("role") Role role, @Param("updatedAt") LocalDateTime updatedAt);
}
