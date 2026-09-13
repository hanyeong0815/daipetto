package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationDetailResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationSummary;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetReservationServiceTest {

    private ReservationRepository reservationRepository;
    private HospitalRepository hospitalRepository;
    private PetRepository petRepository;
    private HospitalScheduleRepository hospitalScheduleRepository;
    private GetReservationService getReservationService;

    private final Long userId = 1L;
    private final Long reservationId = 1L;
    private final Long hospitalId = 100L;
    private final Long petId = 10L;
    private final Long scheduleId = 1000L;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        petRepository = mock(PetRepository.class);
        hospitalScheduleRepository = mock(HospitalScheduleRepository.class);
        getReservationService = new GetReservationService(reservationRepository, hospitalRepository, petRepository, hospitalScheduleRepository);
    }

    private Reservation reservation(Long ownerId, ReservationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Reservation(reservationId, ownerId, petId, hospitalId, scheduleId,
                LocalDateTime.of(2026, 5, 20, 10, 0), status, "咳があります", now, now, null);
    }

    private void mockRelatedEntities() {
        LocalDateTime now = LocalDateTime.now();
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(
                new Hospital(hospitalId, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678", HospitalStatus.ACTIVE, now, now, null)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(
                new Pet(petId, userId, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, null, now, now, null)));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(
                new HospitalSchedule(scheduleId, hospitalId, LocalDate.of(2026, 5, 20),
                        LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, now, now)));
    }

    @Test
    @DisplayName("予約一覧取得成功時、ユーザーの予約一覧を返却する")
    void get_reservation_list_success() {
        // given
        when(reservationRepository.findAllByUserId(userId)).thenReturn(List.of(reservation(userId, ReservationStatus.REQUESTED)));
        mockRelatedEntities();

        // when
        List<ReservationSummary> result = getReservationService.execute(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reservationId()).isEqualTo(reservationId);
        assertThat(result.get(0).hospitalName()).isEqualTo("Tokyo Animal Hospital");
        assertThat(result.get(0).petName()).isEqualTo("Momo");
        assertThat(result.get(0).status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @DisplayName("RSV-T006: 予約詳細取得成功時、詳細を返却する")
    void get_reservation_detail_success() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(userId, ReservationStatus.REQUESTED)));
        mockRelatedEntities();

        // when
        ReservationDetailResponse result = getReservationService.execute(userId, reservationId);

        // then
        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.hospitalName()).isEqualTo("Tokyo Animal Hospital");
        assertThat(result.petName()).isEqualTo("Momo");
        assertThat(result.memo()).isEqualTo("咳があります");
        assertThat(result.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @DisplayName("存在しない予約の詳細取得時、RESERVATION-006例外が発生する")
    void get_reservation_detail_fail_when_not_found() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getReservationService.execute(userId, reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("他人の予約詳細取得時、RESERVATION-007例外が発生する")
    void get_reservation_detail_fail_when_not_owner() {
        // given
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(999L, ReservationStatus.REQUESTED)));

        // when & then
        assertThatThrownBy(() -> getReservationService.execute(userId, reservationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.NOT_RESERVATION_OWNER.code()));
    }
}
