package koh.portfolio.springapi.presentation.reservation;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.AdminReservationResponse;
import koh.portfolio.springapi.application.reservation.usecase.ApproveReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetAdminReservationListUseCase;
import koh.portfolio.springapi.application.reservation.usecase.RejectReservationUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
public class ReservationAdminController {
    private final ApproveReservationUseCase approveReservationUseCase;
    private final CompleteReservationUseCase completeReservationUseCase;
    private final RejectReservationUseCase rejectReservationUseCase;
    private final GetAdminReservationListUseCase getAdminReservationListUseCase;

    // SC-013 予約管理: 病院ごとの予約一覧（病院管理者は紐付いた病院のみ）
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminReservationResponse>>> getReservationList(
            Authentication authentication,
            @RequestParam Long hospitalId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                getAdminReservationListUseCase.execute(extractUserId(authentication), hospitalId, status, date)));
    }

    @PatchMapping("/{reservationId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReservation(Authentication authentication, @PathVariable Long reservationId) {
        approveReservationUseCase.execute(extractUserId(authentication), reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{reservationId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeReservation(Authentication authentication, @PathVariable Long reservationId) {
        completeReservationUseCase.execute(extractUserId(authentication), reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{reservationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReservation(Authentication authentication, @PathVariable Long reservationId) {
        rejectReservationUseCase.execute(extractUserId(authentication), reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
