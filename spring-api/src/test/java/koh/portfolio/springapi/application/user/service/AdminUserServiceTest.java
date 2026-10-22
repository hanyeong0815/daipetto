package koh.portfolio.springapi.application.user.service;

import koh.portfolio.springapi.application.user.dto.AdminUserDto.AdminUserResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.response.PageResponse;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private HospitalAdminRepository hospitalAdminRepository;
    private SearchUsersService searchUsersService;
    private ChangeUserStatusService changeUserStatusService;
    private ChangeUserRoleService changeUserRoleService;

    private final Long adminId = 1L;
    private final Long userId = 10L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        hospitalAdminRepository = mock(HospitalAdminRepository.class);
        searchUsersService = new SearchUsersService(userRepository);
        changeUserStatusService = new ChangeUserStatusService(userRepository, refreshTokenRepository);
        changeUserRoleService = new ChangeUserRoleService(userRepository, hospitalAdminRepository);
    }

    private void assertErrorCode(ThrowingCallable call, UserErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code()).isEqualTo(errorCode.code()));
    }

    // ------------------------------------------------------------------ search

    @Test
    @DisplayName("ユーザー検索結果をパスワードを含まない応答に変換し、ページ情報を付ける")
    void search_users_maps_and_pages() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 12, 0);
        User user = new User(userId, "tanaka@example.com", "$2a$secret", "Tanaka", Role.ROLE_USER, UserStatus.ACTIVE,
                createdAt, createdAt, null);
        when(userRepository.search("tanaka", Role.ROLE_USER, UserStatus.ACTIVE, 1, 2)).thenReturn(List.of(user));
        when(userRepository.count("tanaka", Role.ROLE_USER, UserStatus.ACTIVE)).thenReturn(3L);

        // when
        PageResponse<AdminUserResponse> page = searchUsersService.execute(" tanaka ", Role.ROLE_USER, UserStatus.ACTIVE, 1, 2);

        // then
        assertThat(page.content()).containsExactly(new AdminUserResponse(userId, "tanaka@example.com", "Tanaka",
                Role.ROLE_USER, UserStatus.ACTIVE, createdAt));
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.number()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("範囲外のページ指定は丸め、空白のキーワードは条件なしとして扱う")
    void search_users_clamps_paging_and_blank_keyword() {
        // given
        when(userRepository.search(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        // when
        searchUsersService.execute("  ", null, null, -3, 0);
        searchUsersService.execute(null, null, null, 0, 1000);

        // then
        verify(userRepository).search(isNull(), isNull(), isNull(), eq(0), eq(SearchUsersService.DEFAULT_SIZE));
        verify(userRepository).search(isNull(), isNull(), isNull(), eq(0), eq(SearchUsersService.MAX_SIZE));
    }

    // ------------------------------------------------------------------ status

    @Test
    @DisplayName("停止時はユーザー行を排他取得してから状態を更新し、Refresh Tokenをすべて失効させる")
    void suspend_user_locks_updates_and_revokes() {
        // given
        when(userRepository.updateStatus(eq(userId), eq(UserStatus.SUSPENDED), any())).thenReturn(true);

        // when
        changeUserStatusService.execute(adminId, userId, UserStatus.SUSPENDED);

        // then
        InOrder order = inOrder(userRepository, refreshTokenRepository);
        order.verify(userRepository).lockForSessionUpdate(userId);
        order.verify(userRepository).updateStatus(eq(userId), eq(UserStatus.SUSPENDED), any(LocalDateTime.class));
        order.verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    @DisplayName("復旧時は状態だけを更新し、トークンは失効させない")
    void activate_user_does_not_revoke() {
        // given
        when(userRepository.updateStatus(eq(userId), eq(UserStatus.ACTIVE), any())).thenReturn(true);

        // when
        changeUserStatusService.execute(adminId, userId, UserStatus.ACTIVE);

        // then
        verify(userRepository).updateStatus(eq(userId), eq(UserStatus.ACTIVE), any(LocalDateTime.class));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("自分自身の停止はUSER-005例外が発生し、何も更新しない")
    void change_status_fail_when_self() {
        // when & then
        assertErrorCode(() -> changeUserStatusService.execute(adminId, adminId, UserStatus.SUSPENDED),
                UserErrorCode.CANNOT_CHANGE_SELF);
        verifyNoInteractions(userRepository, refreshTokenRepository);
    }

    @Test
    @DisplayName("存在しない・論理削除済みユーザーの停止はUSER-003例外が発生し、トークンは失効させない")
    void change_status_fail_when_not_found() {
        // given
        when(userRepository.updateStatus(anyLong(), any(), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> changeUserStatusService.execute(adminId, userId, UserStatus.SUSPENDED),
                UserErrorCode.NO_SUCH_USER);
        verifyNoInteractions(refreshTokenRepository);
    }

    // -------------------------------------------------------------------- role

    @Test
    @DisplayName("権限変更は対象ユーザーの権限列だけを更新する")
    void change_role_success() {
        // given
        when(userRepository.updateRole(eq(userId), eq(Role.ROLE_HOSPITAL_ADMIN), any())).thenReturn(true);

        // when
        changeUserRoleService.execute(adminId, userId, Role.ROLE_HOSPITAL_ADMIN);

        // then
        verify(userRepository).updateRole(eq(userId), eq(Role.ROLE_HOSPITAL_ADMIN), any(LocalDateTime.class));
        verify(hospitalAdminRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    @DisplayName("病院管理者以外の権限に変更すると、病院の紐付けをすべて外す")
    void change_role_away_from_hospital_admin_removes_links() {
        // given
        when(userRepository.updateRole(eq(userId), eq(Role.ROLE_USER), any())).thenReturn(true);

        // when
        changeUserRoleService.execute(adminId, userId, Role.ROLE_USER);

        // then
        verify(hospitalAdminRepository).deleteAllByUserId(userId);
    }

    @Test
    @DisplayName("自分自身の権限変更はUSER-005例外、存在しないユーザーはUSER-003例外が発生する")
    void change_role_fail_when_self_or_not_found() {
        // given
        when(userRepository.updateRole(anyLong(), any(), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> changeUserRoleService.execute(adminId, adminId, Role.ROLE_USER), UserErrorCode.CANNOT_CHANGE_SELF);
        assertErrorCode(() -> changeUserRoleService.execute(adminId, userId, Role.ROLE_USER), UserErrorCode.NO_SUCH_USER);
        verify(userRepository, never()).updateRole(eq(adminId), any(), any());
    }
}
