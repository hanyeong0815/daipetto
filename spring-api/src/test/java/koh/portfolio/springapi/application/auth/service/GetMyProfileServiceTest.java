package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.MyProfileResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMyProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GetMyProfileService getMyProfileService = new GetMyProfileService(userRepository);

    @Test
    @DisplayName("ユーザーIDで自分のプロフィールを取得する")
    void get_my_profile_success() {
        // given
        Long userId = 1L;

        User user = new User(
                userId,
                "test@example.com",
                "$2a$encoded",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        MyProfileResponse response = getMyProfileService.execute(userId);

        // then
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("hanyeong");
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("存在しないユーザーIDの場合、USER-003例外が発生する")
    void get_my_profile_fail_when_user_not_found() {
        // given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getMyProfileService.execute(userId))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(UserErrorCode.NO_SUCH_USER.code());
                });

        verify(userRepository).findById(userId);
    }
}