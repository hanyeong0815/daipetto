package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationRequest;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateReservationServiceTest {

    private PetRepository petRepository;
    private HospitalScheduleRepository hospitalScheduleRepository;
    private ReservationRepository reservationRepository;
    private CreateReservationService createReservationService;

    private final Long userId = 1L;
    private final Long petId = 10L;
    private final Long hospitalId = 100L;
    private final Long scheduleId = 1000L;

    @BeforeEach
    void setUp() {
        petRepository = mock(PetRepository.class);
        hospitalScheduleRepository = mock(HospitalScheduleRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        createReservationService = new CreateReservationService(petRepository, hospitalScheduleRepository, reservationRepository);
    }

    private Pet ownedPet() {
        return new Pet(petId, userId, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private HospitalSchedule availableFutureSchedule() {
        return new HospitalSchedule(scheduleId, hospitalId, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("RSV-T001: 正常な予約申請時、REQUESTED状態の予約を作成する")
    void create_reservation_success() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, "咳があります");

        when(petRepository.findById(petId)).thenReturn(Optional.of(ownedPet()));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(availableFutureSchedule()));
        when(reservationRepository.existsActiveByScheduleId(scheduleId)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation arg = invocation.getArgument(0);
            return new Reservation(1L, arg.getUserId(), arg.getPetId(), arg.getHospitalId(), arg.getScheduleId(),
                    arg.getReservationDatetime(), arg.getStatus(), arg.getMemo(), arg.getCreatedAt(), arg.getUpdatedAt(), null);
        });

        // when
        CreateReservationResponse response = createReservationService.execute(userId, request);

        // then
        assertThat(response.reservationId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @DisplayName("RSV-T002: 同一予約枠への重複予約時、RESERVATION-001例外が発生する")
    void create_reservation_fail_when_duplicated() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(ownedPet()));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(availableFutureSchedule()));
        when(reservationRepository.existsActiveByScheduleId(scheduleId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_DUPLICATED.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("RSV-T003: 他人のペットで予約申請時、RESERVATION-004例外が発生する")
    void create_reservation_fail_when_not_pet_owner() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);
        Pet othersPet = new Pet(petId, 999L, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, null,
                LocalDateTime.now(), LocalDateTime.now(), null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(othersPet));

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.NOT_PET_OWNER.code()));

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("存在しないペットで予約申請時、RESERVATION-004例外が発生する")
    void create_reservation_fail_when_pet_not_found() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.NOT_PET_OWNER.code()));
    }

    @Test
    @DisplayName("指定した病院に属さない予約枠で申請時、RESERVATION-002例外が発生する")
    void create_reservation_fail_when_schedule_hospital_mismatch() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);
        HospitalSchedule schedule = new HospitalSchedule(scheduleId, 999L, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, LocalDateTime.now(), LocalDateTime.now());

        when(petRepository.findById(petId)).thenReturn(Optional.of(ownedPet()));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_SCHEDULE_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("BLOCKED状態の予約枠への申請時、RESERVATION-003例外が発生する")
    void create_reservation_fail_when_schedule_blocked() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);
        HospitalSchedule blockedSchedule = new HospitalSchedule(scheduleId, hospitalId, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.BLOCKED, LocalDateTime.now(), LocalDateTime.now());

        when(petRepository.findById(petId)).thenReturn(Optional.of(ownedPet()));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(blockedSchedule));

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.RESERVATION_SCHEDULE_BLOCKED.code()));
    }

    @Test
    @DisplayName("過去日時の予約枠への申請時、RESERVATION-005例外が発生する")
    void create_reservation_fail_when_past_datetime() {
        // given
        CreateReservationRequest request = new CreateReservationRequest(petId, hospitalId, scheduleId, null);
        HospitalSchedule pastSchedule = new HospitalSchedule(scheduleId, hospitalId, LocalDate.now().minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, LocalDateTime.now(), LocalDateTime.now());

        when(petRepository.findById(petId)).thenReturn(Optional.of(ownedPet()));
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(pastSchedule));

        // when & then
        assertThatThrownBy(() -> createReservationService.execute(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(ReservationErrorCode.PAST_DATETIME.code()));
    }
}
