package koh.portfolio.springapi.presentation.reservation;

import koh.portfolio.springapi.application.reservation.usecase.ApproveReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class ReservationAdminController {
    private final ApproveReservationUseCase approveReservationUseCase;
    private final CompleteReservationUseCase completeReservationUseCase;

    @PatchMapping("/{reservationId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveReservation(@PathVariable Long reservationId) {
        approveReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{reservationId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeReservation(@PathVariable Long reservationId) {
        completeReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
