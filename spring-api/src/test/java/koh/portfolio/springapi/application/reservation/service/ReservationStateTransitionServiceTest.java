package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationStateTransitionServiceTest {

    private ReservationRepository reservationRepository;
    private ApproveReservationService approveReservationService;
    private CancelReservationService cancelReservationService;
    private CompleteReservationService completeReservationService;

    private final Long userId = 1L;
    private final Long reservationId = 1L;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        approveReservationService = new ApproveReservationService(reservationRepository);
        cancelReservationService = new CancelReservationService(reservationRepository);
        completeReservationService = new CompleteReservationService(reservationRepository);
    }

    private Reservation reservation(ReservationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Reservation(reservationId, userId, 10L, 100L, 1000L, now, status, null, now, now, null);
    }

    private void stubFindAndSave(ReservationStatus currentStatus) {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(currentStatus)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ----------------------------------------------------------------- approve

    @Test
    @DisplayName("STATE-T001: REQUESTED状態の予約承認時、APPROVEDへ遷移する")
    void approve_success_when_requested() {
        // given
        stubFindAndSave(ReservationStatus.REQUESTED);

        // when
        approveReservationService.execute(reservationId);

        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    @DisplayName("STATE-T006: COMPLETED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_completed() {
        // given
        stubFindAndSave(ReservationStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> approveReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));
    }

    @Test
    @DisplayName("STATE-T007: REJECTED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_rejected() {
        // given
        stubFindAndSave(ReservationStatus.REJECTED);

        // when & then
        assertThatThrownBy(() -> approveReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));
    }

    @Test
    @DisplayName("STATE-T008: CANCELLED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_cancelled() {
        // given
        stubFindAndSave(ReservationStatus.CANCELLED);

        // when & then
        assertThatThrownBy(() -> approveReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));
    }

    @Test
    @DisplayName("存在しない予約の承認時、RESERVATION-006例外が発生する")
    void approve_fail_when_not_found() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> approveReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    // ------------------------------------------------------------------ cancel

    @Test
    @DisplayName("RSV-T004・STATE-T003: REQUESTED状態の予約キャンセル時、CANCELLEDへ遷移する")
    void cancel_success_when_requested() {
        // given
        stubFindAndSave(ReservationStatus.REQUESTED);

        // when
        cancelReservationService.execute(userId, reservationId);

        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("STATE-T005: APPROVED状態の予約キャンセル時、CANCELLEDへ遷移する")
    void cancel_success_when_approved() {
        // given
        stubFindAndSave(ReservationStatus.APPROVED);

        // when
        cancelReservationService.execute(userId, reservationId);

        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("RSV-T005: COMPLETED状態の予約キャンセル時、RESERVATION-008例外が発生する")
    void cancel_fail_when_completed() {
        // given
        stubFindAndSave(ReservationStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> cancelReservationService.execute(userId, reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("他人の予約キャンセル時、RESERVATION-007例外が発生する")
    void cancel_fail_when_not_owner() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(ReservationStatus.REQUESTED)));

        // when & then
        assertThatThrownBy(() -> cancelReservationService.execute(999L, reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.NOT_RESERVATION_OWNER.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    // ---------------------------------------------------------------- complete

    @Test
    @DisplayName("STATE-T004: APPROVED状態の診療完了時、COMPLETEDへ遷移する")
    void complete_success_when_approved() {
        // given
        stubFindAndSave(ReservationStatus.APPROVED);

        // when
        completeReservationService.execute(reservationId);

        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("REQUESTED状態の診療完了時、RESERVATION-008例外が発生する")
    void complete_fail_when_requested() {
        // given
        stubFindAndSave(ReservationStatus.REQUESTED);

        // when & then
        assertThatThrownBy(() -> completeReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }
}
