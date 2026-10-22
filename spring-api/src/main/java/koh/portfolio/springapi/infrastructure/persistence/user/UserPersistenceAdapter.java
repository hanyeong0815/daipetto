package koh.portfolio.springapi.infrastructure.persistence.user;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity saveEntity = userJpaRepository.save(
                userMapper.toEntity(user)
        );
        return userMapper.toDomain(saveEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public void lockForSessionUpdate(Long userId) {
        userJpaRepository.findByIdForUpdate(userId);
    }

    @Override
    public UserStatus findStatusById(Long id) {
        return userJpaRepository.findStatusById(id).orElse(null);
    }

    @Override
    public List<User> search(String keyword, Role role, UserStatus status, int page, int size) {
        return userJpaRepository.search(keyword, role, status, PageRequest.of(page, size)).stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String keyword, Role role, UserStatus status) {
        return userJpaRepository.countBySearch(keyword, role, status);
    }

    @Override
    public boolean updateStatus(Long id, UserStatus status, LocalDateTime updatedAt) {
        return userJpaRepository.updateStatus(id, status, updatedAt) == 1;
    }

    @Override
    public boolean updateRole(Long id, Role role, LocalDateTime updatedAt) {
        return userJpaRepository.updateRole(id, role, updatedAt) == 1;
    }
}
