package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationRequest;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationResponse;
import koh.portfolio.springapi.application.reservation.usecase.CreateReservationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateReservationService implements CreateReservationUseCase {
    private final PetRepository petRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public CreateReservationResponse execute(Long userId, CreateReservationRequest request) {
        Pet pet = petRepository.findById(request.petId()).orElse(null);
        Preconditions.validate(pet != null && pet.getUserId().equals(userId), ReservationErrorCode.NOT_PET_OWNER);

        HospitalSchedule schedule = hospitalScheduleRepository.findById(request.scheduleId()).orElse(null);
        Preconditions.validate(
                schedule != null && schedule.getHospitalId().equals(request.hospitalId()),
                ReservationErrorCode.RESERVATION_SCHEDULE_NOT_FOUND
        );

        Preconditions.validate(schedule.getStatus() != HospitalScheduleStatus.BLOCKED, ReservationErrorCode.RESERVATION_SCHEDULE_BLOCKED);

        LocalDateTime reservationDatetime = LocalDateTime.of(schedule.getAvailableDate(), schedule.getStartTime());
        Preconditions.validate(reservationDatetime.isAfter(LocalDateTime.now()), ReservationErrorCode.PAST_DATETIME);

        Preconditions.validate(
                !reservationRepository.existsActiveByScheduleId(request.scheduleId()),
                ReservationErrorCode.RESERVATION_DUPLICATED
        );

        Reservation reservation = Reservation.create(
                userId, request.petId(), request.hospitalId(), request.scheduleId(), reservationDatetime, request.memo()
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        return CreateReservationResponse.builder()
                .reservationId(savedReservation.getId())
                .status(savedReservation.getStatus())
                .build();
    }
}
