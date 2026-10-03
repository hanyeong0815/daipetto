package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationPersistenceAdapterTest {

    private ReservationJpaRepository reservationJpaRepository;
    private ReservationPersistenceAdapter reservationPersistenceAdapter;

    @BeforeEach
    void setUp() {
        reservationJpaRepository = mock(ReservationJpaRepository.class);
        reservationPersistenceAdapter = new ReservationPersistenceAdapter(
                reservationJpaRepository, new ReservationMapperImpl());
    }

    private Reservation newReservation() {
        LocalDateTime now = LocalDateTime.now();
        return new Reservation(null, 1L, 10L, 100L, 1000L, now.plusDays(1),
                ReservationStatus.REQUESTED, null, now, now, null);
    }

    @Test
    @DisplayName("予約枠の一意制約に違反した場合、RESERVATION-001例外に変換する")
    void save_translates_unique_violation_to_duplicated_error() {
        // given
        when(reservationJpaRepository.saveAndFlush(any(ReservationEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uq_reservations_active_schedule"));

        // when & then
        assertThatThrownBy(() -> reservationPersistenceAdapter.save(newReservation()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_DUPLICATED.code()));
    }

    @Test
    @DisplayName("予約枠の一意制約以外の整合性違反は変換せずそのまま伝播する")
    void save_rethrows_unrelated_integrity_violation() {
        // given
        DataIntegrityViolationException unrelated =
                new DataIntegrityViolationException("violates foreign key constraint \"fk_reservations_pet\"");
        when(reservationJpaRepository.saveAndFlush(any(ReservationEntity.class))).thenThrow(unrelated);

        // when & then
        assertThatThrownBy(() -> reservationPersistenceAdapter.save(newReservation()))
                .isSameAs(unrelated);
    }

    @Test
    @DisplayName("条件付き更新が1件成立した場合、trueを返却する")
    void update_status_returns_true_when_one_row_updated() {
        // given
        LocalDateTime updatedAt = LocalDateTime.now();
        when(reservationJpaRepository.updateStatus(1L, ReservationStatus.REQUESTED, ReservationStatus.APPROVED, updatedAt))
                .thenReturn(1);

        // when
        boolean result = reservationPersistenceAdapter.updateStatus(
                1L, ReservationStatus.REQUESTED, ReservationStatus.APPROVED, updatedAt);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("条件付き更新が0件の場合、falseを返却する（他の遷移が先に成立）")
    void update_status_returns_false_when_no_row_matched() {
        // given
        LocalDateTime updatedAt = LocalDateTime.now();
        when(reservationJpaRepository.updateStatus(1L, ReservationStatus.REQUESTED, ReservationStatus.APPROVED, updatedAt))
                .thenReturn(0);

        // when
        boolean result = reservationPersistenceAdapter.updateStatus(
                1L, ReservationStatus.REQUESTED, ReservationStatus.APPROVED, updatedAt);

        // then
        assertThat(result).isFalse();
    }
}
