package koh.portfolio.springapi.infrastructure.persistence.user;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        UserPersistenceAdapter.class,
        UserMapperImpl.class
})
class UserPersistenceAdapterTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("セッション更新用のユーザー行排他取得が実DBで実行できる")
    void lock_for_session_update_runs_against_database() {
        // given
        LocalDateTime now = LocalDateTime.now();
        UserEntity saved = userJpaRepository.save(UserEntity.builder()
                .email("lock@example.com")
                .password("$2a$encoded-password")
                .nickname("locker")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        // when & then
        assertThatCode(() -> userRepository.lockForSessionUpdate(saved.getId())).doesNotThrowAnyException();
        assertThatCode(() -> userRepository.lockForSessionUpdate(999_999L)).doesNotThrowAnyException();
    }
}
