package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.hospital.service.HospitalAccessChecker;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.AdminReservationResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.reservation.model.AdminReservationView;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetAdminReservationListServiceTest {

    private final HospitalAccessChecker hospitalAccessChecker = mock(HospitalAccessChecker.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final GetAdminReservationListService service =
            new GetAdminReservationListService(hospitalAccessChecker, reservationRepository);

    @Test
    @DisplayName("管理対象の確認後に、病院・状態・日付で一覧を取得して応答に変換する")
    void get_admin_reservation_list_success() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 25);
        when(reservationRepository.findAllForAdmin(1L, ReservationStatus.REQUESTED, date)).thenReturn(List.of(
                new AdminReservationView(7L, ReservationStatus.REQUESTED, date, LocalTime.of(10, 0), LocalTime.of(10, 30),
                        3L, "Tanaka", "Momo", PetType.CAT, "咳", LocalDateTime.of(2026, 10, 20, 9, 0))));

        // when
        List<AdminReservationResponse> result = service.execute(99L, 1L, ReservationStatus.REQUESTED, date);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reservationId()).isEqualTo(7L);
        assertThat(result.get(0).userNickname()).isEqualTo("Tanaka");
        assertThat(result.get(0).petType()).isEqualTo(PetType.CAT);
        InOrder order = inOrder(hospitalAccessChecker, reservationRepository);
        order.verify(hospitalAccessChecker).checkManageable(99L, 1L);
        order.verify(reservationRepository).findAllForAdmin(1L, ReservationStatus.REQUESTED, date);
    }

    @Test
    @DisplayName("管理対象外の病院はHOSPITAL-005となり、一覧を取得しない")
    void get_admin_reservation_list_fail_when_not_manager() {
        // given
        doThrow(HospitalErrorCode.NOT_HOSPITAL_MANAGER.defaultException()).when(hospitalAccessChecker).checkManageable(99L, 2L);

        // when & then
        assertThatThrownBy(() -> service.execute(99L, 2L, null, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HospitalErrorCode.NOT_HOSPITAL_MANAGER.code()));
        verifyNoInteractions(reservationRepository);
    }
}
