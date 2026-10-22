package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.hospital.service.HospitalAccessChecker;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.AdminReservationResponse;
import koh.portfolio.springapi.application.reservation.usecase.GetAdminReservationListUseCase;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAdminReservationListService implements GetAdminReservationListUseCase {
    private final HospitalAccessChecker hospitalAccessChecker;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminReservationResponse> execute(Long userId, Long hospitalId, ReservationStatus status, LocalDate date) {
        // 病院管理者は紐付いた病院の予約だけを閲覧できる（06_ERD §11）
        hospitalAccessChecker.checkManageable(userId, hospitalId);

        return reservationRepository.findAllForAdmin(hospitalId, status, date).stream()
                .map(view -> AdminReservationResponse.builder()
                        .reservationId(view.reservationId())
                        .status(view.status())
                        .availableDate(view.availableDate())
                        .startTime(view.startTime())
                        .endTime(view.endTime())
                        .userId(view.userId())
                        .userNickname(view.userNickname())
                        .petName(view.petName())
                        .petType(view.petType())
                        .memo(view.memo())
                        .createdAt(view.createdAt())
                        .build())
                .toList();
    }
}
