package koh.portfolio.springapi.infrastructure.persistence.user;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void domainを_entityに_変換() {
        // given
        LocalDateTime now = LocalDateTime.now();

        User domain = new User(
                1L,
                "test@example.com",
                "$2a$encoded",
                "testUser",
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                now,
                now,
                null
        );

        // when
        UserEntity entity = userMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
        assertThat(entity.getNickname()).isEqualTo("testUser");
        assertThat(entity.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
