package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationDetailResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationSummary;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationDetailUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationListUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetReservationService implements GetReservationListUseCase, GetReservationDetailUseCase {
    private final ReservationRepository reservationRepository;
    private final HospitalRepository hospitalRepository;
    private final PetRepository petRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Override
    public List<ReservationSummary> execute(Long userId) {
        List<Reservation> reservations = reservationRepository.findAllByUserId(userId);

        return reservations.stream()
                .map(reservation -> {
                    Hospital hospital = findHospital(reservation.getHospitalId());
                    Pet pet = findPet(reservation.getPetId());
                    HospitalSchedule schedule = findSchedule(reservation.getScheduleId());

                    return ReservationSummary.builder()
                            .reservationId(reservation.getId())
                            .hospitalName(hospital.getName())
                            .petName(pet.getName())
                            .availableDate(schedule.getAvailableDate())
                            .startTime(schedule.getStartTime())
                            .endTime(schedule.getEndTime())
                            .status(reservation.getStatus())
                            .build();
                })
                .toList();
    }

    @Override
    public ReservationDetailResponse execute(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        Preconditions.validate(reservation.getUserId().equals(userId), ReservationErrorCode.NOT_RESERVATION_OWNER);

        Hospital hospital = findHospital(reservation.getHospitalId());
        Pet pet = findPet(reservation.getPetId());
        HospitalSchedule schedule = findSchedule(reservation.getScheduleId());

        return ReservationDetailResponse.builder()
                .reservationId(reservation.getId())
                .hospitalName(hospital.getName())
                .petName(pet.getName())
                .availableDate(schedule.getAvailableDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .status(reservation.getStatus())
                .memo(reservation.getMemo())
                .build();
    }

    private Hospital findHospital(Long hospitalId) {
        return hospitalRepository.findById(hospitalId).orElseThrow(ReservationErrorCode.RESERVATION_SCHEDULE_NOT_FOUND::defaultException);
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId).orElseThrow(ReservationErrorCode.NOT_PET_OWNER::defaultException);
    }

    private HospitalSchedule findSchedule(Long scheduleId) {
        return hospitalScheduleRepository.findById(scheduleId).orElseThrow(ReservationErrorCode.RESERVATION_SCHEDULE_NOT_FOUND::defaultException);
    }
}
