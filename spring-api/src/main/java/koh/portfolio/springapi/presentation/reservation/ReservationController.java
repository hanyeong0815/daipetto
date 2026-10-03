package koh.portfolio.springapi.presentation.reservation;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationRequest;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationDetailResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationSummary;
import koh.portfolio.springapi.application.reservation.usecase.CancelReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CreateReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationDetailUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationListUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationListUseCase getReservationListUseCase;
    private final GetReservationDetailUseCase getReservationDetailUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateReservationResponse>> createReservation(
            Authentication authentication,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createReservationUseCase.execute(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationSummary>>> getReservationList(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getReservationListUseCase.execute(userId)));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            Authentication authentication,
            @PathVariable Long reservationId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getReservationDetailUseCase.execute(userId, reservationId)));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            Authentication authentication,
            @PathVariable Long reservationId
    ) {
        Long userId = extractUserId(authentication);
        cancelReservationUseCase.execute(userId, reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
