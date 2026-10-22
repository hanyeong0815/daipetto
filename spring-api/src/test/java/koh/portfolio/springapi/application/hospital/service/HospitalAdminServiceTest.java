package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.ManagedHospitalResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class HospitalAdminServiceTest {

    private UserRepository userRepository;
    private HospitalRepository hospitalRepository;
    private HospitalAdminRepository hospitalAdminRepository;
    private HospitalAccessChecker hospitalAccessChecker;
    private GetManagedHospitalsService getManagedHospitalsService;
    private AssignHospitalAdminService assignHospitalAdminService;
    private UnassignHospitalAdminService unassignHospitalAdminService;

    private final Long hospitalId = 1L;
    private final Long userId = 5L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        hospitalAdminRepository = mock(HospitalAdminRepository.class);
        hospitalAccessChecker = new HospitalAccessChecker(userRepository, hospitalAdminRepository);
        getManagedHospitalsService = new GetManagedHospitalsService(hospitalAccessChecker, hospitalRepository, hospitalAdminRepository);
        assignHospitalAdminService = new AssignHospitalAdminService(hospitalRepository, userRepository, hospitalAdminRepository);
        unassignHospitalAdminService = new UnassignHospitalAdminService(hospitalAdminRepository);
    }

    private User user(Long id, Role role, LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        return new User(id, "u" + id + "@example.com", "pw", "U" + id, role, UserStatus.ACTIVE, now, now, deletedAt);
    }

    private Hospital hospital(Long id, HospitalStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Hospital(id, "病院" + id, "東京都", "03", status, now, now, null);
    }

    private void assertErrorCode(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code()).isEqualTo(errorCode.code()));
    }

    // ------------------------------------------------------------- access checker

    @Test
    @DisplayName("システム管理者はすべての病院、病院管理者は紐付いた病院だけを管理できる（それ以外はHOSPITAL-005）")
    void check_manageable_by_role_and_link() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ROLE_SYSTEM_ADMIN, null)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, Role.ROLE_HOSPITAL_ADMIN, null)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, Role.ROLE_USER, null)));
        when(hospitalAdminRepository.exists(userId, hospitalId)).thenReturn(true);

        // when & then
        assertThatCode(() -> hospitalAccessChecker.checkManageable(1L, 99L)).doesNotThrowAnyException();
        assertThatCode(() -> hospitalAccessChecker.checkManageable(userId, hospitalId)).doesNotThrowAnyException();
        assertErrorCode(() -> hospitalAccessChecker.checkManageable(userId, 2L), HospitalErrorCode.NOT_HOSPITAL_MANAGER);
        assertErrorCode(() -> hospitalAccessChecker.checkManageable(9L, hospitalId), HospitalErrorCode.NOT_HOSPITAL_MANAGER);
        verify(hospitalAdminRepository, never()).exists(9L, hospitalId);
    }

    @Test
    @DisplayName("存在しないユーザーはAUTH-005")
    void check_manageable_fail_when_user_missing() {
        // given
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> hospitalAccessChecker.checkManageable(42L, hospitalId), AuthErrorCode.ACCESS_DENIED);
    }

    // ----------------------------------------------------------- managed hospitals

    @Test
    @DisplayName("管理対象の病院: システム管理者は停止中を含む全病院、病院管理者は紐付いた病院だけ")
    void get_managed_hospitals_by_role() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ROLE_SYSTEM_ADMIN, null)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, Role.ROLE_HOSPITAL_ADMIN, null)));
        when(hospitalRepository.findAllForManagement()).thenReturn(List.of(hospital(1L, HospitalStatus.ACTIVE), hospital(2L, HospitalStatus.SUSPENDED)));
        when(hospitalAdminRepository.findHospitalIdsByUserId(userId)).thenReturn(List.of(2L));
        when(hospitalRepository.findAllForManagement(List.of(2L))).thenReturn(List.of(hospital(2L, HospitalStatus.SUSPENDED)));

        // when
        List<ManagedHospitalResponse> forSystemAdmin = getManagedHospitalsService.execute(1L);
        List<ManagedHospitalResponse> forHospitalAdmin = getManagedHospitalsService.execute(userId);

        // then
        assertThat(forSystemAdmin).extracting(ManagedHospitalResponse::hospitalId).containsExactly(1L, 2L);
        assertThat(forSystemAdmin.get(1).status()).isEqualTo(HospitalStatus.SUSPENDED);
        assertThat(forHospitalAdmin).extracting(ManagedHospitalResponse::hospitalId).containsExactly(2L);
    }

    // ------------------------------------------------------------------ assign

    @Test
    @DisplayName("病院管理者権限のユーザーを病院に紐付ける")
    void assign_hospital_admin_success() {
        // given
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital(hospitalId, HospitalStatus.ACTIVE)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, Role.ROLE_HOSPITAL_ADMIN, null)));

        // when
        assignHospitalAdminService.execute(hospitalId, userId);

        // then: 権限変更と直列化するため、ユーザー行を排他取得してから権限を読む
        InOrder order = inOrder(userRepository, hospitalAdminRepository);
        order.verify(userRepository).lockForSessionUpdate(userId);
        order.verify(userRepository).findById(userId);
        ArgumentCaptor<HospitalAdmin> captor = ArgumentCaptor.forClass(HospitalAdmin.class);
        order.verify(hospitalAdminRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getHospitalId()).isEqualTo(hospitalId);
    }

    @Test
    @DisplayName("紐付けの失敗: 病院なしHOSPITAL-001・ユーザーなし/削除済みUSER-003・権限違いHOSPITAL-006・既存HOSPITAL-007")
    void assign_hospital_admin_failures() {
        // given
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital(hospitalId, HospitalStatus.ACTIVE)));
        when(hospitalRepository.findById(2L)).thenReturn(Optional.empty());
        when(userRepository.findById(6L)).thenReturn(Optional.of(user(6L, Role.ROLE_HOSPITAL_ADMIN, LocalDateTime.now())));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Role.ROLE_USER, null)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, Role.ROLE_HOSPITAL_ADMIN, null)));
        when(hospitalAdminRepository.exists(userId, hospitalId)).thenReturn(true);

        // when & then
        assertErrorCode(() -> assignHospitalAdminService.execute(2L, userId), HospitalErrorCode.HOSPITAL_NOT_FOUND);
        assertErrorCode(() -> assignHospitalAdminService.execute(hospitalId, 404L), UserErrorCode.NO_SUCH_USER);
        assertErrorCode(() -> assignHospitalAdminService.execute(hospitalId, 6L), UserErrorCode.NO_SUCH_USER);
        assertErrorCode(() -> assignHospitalAdminService.execute(hospitalId, 7L), HospitalErrorCode.NOT_HOSPITAL_ADMIN_USER);
        assertErrorCode(() -> assignHospitalAdminService.execute(hospitalId, userId), HospitalErrorCode.HOSPITAL_ADMIN_DUPLICATED);
        verify(hospitalAdminRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- unassign

    @Test
    @DisplayName("紐付けの解除。紐付けが無ければHOSPITAL-008")
    void unassign_hospital_admin() {
        // given
        when(hospitalAdminRepository.delete(userId, hospitalId)).thenReturn(true);
        when(hospitalAdminRepository.delete(6L, hospitalId)).thenReturn(false);

        // when & then
        assertThatCode(() -> unassignHospitalAdminService.execute(hospitalId, userId)).doesNotThrowAnyException();
        assertErrorCode(() -> unassignHospitalAdminService.execute(hospitalId, 6L), HospitalErrorCode.HOSPITAL_ADMIN_NOT_FOUND);
        verify(hospitalRepository, never()).findAllForManagement(anyList());
    }
}
