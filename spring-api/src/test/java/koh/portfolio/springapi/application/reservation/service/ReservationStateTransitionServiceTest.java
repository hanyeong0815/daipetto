package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReservationStateTransitionServiceTest {

    private ReservationRepository reservationRepository;
    private ApproveReservationService approveReservationService;
    private CancelReservationService cancelReservationService;
    private CompleteReservationService completeReservationService;
    private RejectReservationService rejectReservationService;

    private final Long userId = 1L;
    private final Long reservationId = 1L;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        approveReservationService = new ApproveReservationService(reservationRepository);
        cancelReservationService = new CancelReservationService(reservationRepository);
        completeReservationService = new CompleteReservationService(reservationRepository);
        rejectReservationService = new RejectReservationService(reservationRepository);
    }

    private Reservation reservation(ReservationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Reservation(reservationId, userId, 10L, 100L, 1000L, now, status, null, now, now, null);
    }

    private void stubFind(ReservationStatus currentStatus) {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(currentStatus)));
    }

    private void stubUpdateStatus(boolean updated) {
        when(reservationRepository.updateStatus(
                anyLong(), any(ReservationStatus.class), any(ReservationStatus.class), any(LocalDateTime.class)))
                .thenReturn(updated);
    }

    private void verifyNoTransitionPersisted() {
        verify(reservationRepository, never())
                .updateStatus(anyLong(), any(ReservationStatus.class), any(ReservationStatus.class), any(LocalDateTime.class));
    }

    private void verifyTransition(ReservationStatus expectedStatus, ReservationStatus newStatus) {
        verify(reservationRepository).updateStatus(
                eq(reservationId), eq(expectedStatus), eq(newStatus), any(LocalDateTime.class));
    }

    private void assertInvalidStateTransition(Runnable execution) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.INVALID_STATE_TRANSITION.code()));
    }

    // ----------------------------------------------------------------- approve

    @Test
    @DisplayName("STATE-T001: REQUESTED状態の予約承認時、APPROVEDへ遷移する")
    void approve_success_when_requested() {
        // given
        stubFind(ReservationStatus.REQUESTED);
        stubUpdateStatus(true);

        // when
        approveReservationService.execute(reservationId);

        // then
        verifyTransition(ReservationStatus.REQUESTED, ReservationStatus.APPROVED);
    }

    @Test
    @DisplayName("STATE-T006: COMPLETED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_completed() {
        // given
        stubFind(ReservationStatus.COMPLETED);

        // when & then
        assertInvalidStateTransition(() -> approveReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("STATE-T007: REJECTED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_rejected() {
        // given
        stubFind(ReservationStatus.REJECTED);

        // when & then
        assertInvalidStateTransition(() -> approveReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("STATE-T008: CANCELLED状態の予約承認時、RESERVATION-008例外が発生する")
    void approve_fail_when_cancelled() {
        // given
        stubFind(ReservationStatus.CANCELLED);

        // when & then
        assertInvalidStateTransition(() -> approveReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("承認の直前に他の遷移が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する")
    void approve_fail_when_status_changed_concurrently() {
        // given
        stubFind(ReservationStatus.REQUESTED);
        stubUpdateStatus(false);

        // when & then
        assertInvalidStateTransition(() -> approveReservationService.execute(reservationId));

        verifyTransition(ReservationStatus.REQUESTED, ReservationStatus.APPROVED);
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

        verifyNoTransitionPersisted();
    }

    // ------------------------------------------------------------------ cancel

    @Test
    @DisplayName("RSV-T004・STATE-T003: REQUESTED状態の予約キャンセル時、CANCELLEDへ遷移する")
    void cancel_success_when_requested() {
        // given
        stubFind(ReservationStatus.REQUESTED);
        stubUpdateStatus(true);

        // when
        cancelReservationService.execute(userId, reservationId);

        // then
        verifyTransition(ReservationStatus.REQUESTED, ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("STATE-T005: APPROVED状態の予約キャンセル時、CANCELLEDへ遷移する")
    void cancel_success_when_approved() {
        // given
        stubFind(ReservationStatus.APPROVED);
        stubUpdateStatus(true);

        // when
        cancelReservationService.execute(userId, reservationId);

        // then
        verifyTransition(ReservationStatus.APPROVED, ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("RSV-T005: COMPLETED状態の予約キャンセル時、RESERVATION-008例外が発生する")
    void cancel_fail_when_completed() {
        // given
        stubFind(ReservationStatus.COMPLETED);

        // when & then
        assertInvalidStateTransition(() -> cancelReservationService.execute(userId, reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("キャンセルの直前に診療完了が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する")
    void cancel_fail_when_status_changed_concurrently() {
        // given
        stubFind(ReservationStatus.APPROVED);
        stubUpdateStatus(false);

        // when & then
        assertInvalidStateTransition(() -> cancelReservationService.execute(userId, reservationId));

        verifyTransition(ReservationStatus.APPROVED, ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("他人の予約キャンセル時、RESERVATION-007例外が発生する")
    void cancel_fail_when_not_owner() {
        // given
        stubFind(ReservationStatus.REQUESTED);

        // when & then
        assertThatThrownBy(() -> cancelReservationService.execute(999L, reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.NOT_RESERVATION_OWNER.code()));

        verifyNoTransitionPersisted();
    }

    // ---------------------------------------------------------------- complete

    @Test
    @DisplayName("STATE-T004: APPROVED状態の診療完了時、COMPLETEDへ遷移する")
    void complete_success_when_approved() {
        // given
        stubFind(ReservationStatus.APPROVED);
        stubUpdateStatus(true);

        // when
        completeReservationService.execute(reservationId);

        // then
        verifyTransition(ReservationStatus.APPROVED, ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("REQUESTED状態の診療完了時、RESERVATION-008例外が発生する")
    void complete_fail_when_requested() {
        // given
        stubFind(ReservationStatus.REQUESTED);

        // when & then
        assertInvalidStateTransition(() -> completeReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("診療完了の直前にキャンセルが成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する")
    void complete_fail_when_status_changed_concurrently() {
        // given
        stubFind(ReservationStatus.APPROVED);
        stubUpdateStatus(false);

        // when & then
        assertInvalidStateTransition(() -> completeReservationService.execute(reservationId));

        verifyTransition(ReservationStatus.APPROVED, ReservationStatus.COMPLETED);
    }

    // ------------------------------------------------------------------ reject

    @Test
    @DisplayName("REQUESTED状態の予約却下時、REJECTEDへ遷移する")
    void reject_success_when_requested() {
        // given
        stubFind(ReservationStatus.REQUESTED);
        stubUpdateStatus(true);

        // when
        rejectReservationService.execute(reservationId);

        // then
        verifyTransition(ReservationStatus.REQUESTED, ReservationStatus.REJECTED);
    }

    @Test
    @DisplayName("APPROVED状態の予約却下時、RESERVATION-008例外が発生する")
    void reject_fail_when_approved() {
        // given
        stubFind(ReservationStatus.APPROVED);

        // when & then
        assertInvalidStateTransition(() -> rejectReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("COMPLETED状態の予約却下時、RESERVATION-008例外が発生する")
    void reject_fail_when_completed() {
        // given
        stubFind(ReservationStatus.COMPLETED);

        // when & then
        assertInvalidStateTransition(() -> rejectReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("CANCELLED状態の予約却下時、RESERVATION-008例外が発生する")
    void reject_fail_when_cancelled() {
        // given
        stubFind(ReservationStatus.CANCELLED);

        // when & then
        assertInvalidStateTransition(() -> rejectReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("REJECTED状態の予約却下時、RESERVATION-008例外が発生する")
    void reject_fail_when_already_rejected() {
        // given
        stubFind(ReservationStatus.REJECTED);

        // when & then
        assertInvalidStateTransition(() -> rejectReservationService.execute(reservationId));

        verifyNoTransitionPersisted();
    }

    @Test
    @DisplayName("却下の直前に承認が成立していた場合、条件付き更新が0件となりRESERVATION-008例外が発生する")
    void reject_fail_when_status_changed_concurrently() {
        // given
        stubFind(ReservationStatus.REQUESTED);
        stubUpdateStatus(false);

        // when & then
        assertInvalidStateTransition(() -> rejectReservationService.execute(reservationId));

        verifyTransition(ReservationStatus.REQUESTED, ReservationStatus.REJECTED);
    }

    @Test
    @DisplayName("存在しない予約の却下時、RESERVATION-006例外が発生する")
    void reject_fail_when_not_found() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rejectReservationService.execute(reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.code()));

        verifyNoTransitionPersisted();
    }
}
