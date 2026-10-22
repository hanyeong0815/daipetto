package koh.portfolio.springapi.presentation.vaccination;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CompleteVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationResponse;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.RescheduleVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.VaccinationResponse;
import koh.portfolio.springapi.application.vaccination.usecase.CancelVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.CompleteVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.CreateVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.DeleteVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.GetVaccinationListUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.RescheduleVaccinationUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VaccinationController {
    private final CreateVaccinationUseCase createVaccinationUseCase;
    private final GetVaccinationListUseCase getVaccinationListUseCase;
    private final CompleteVaccinationUseCase completeVaccinationUseCase;
    private final RescheduleVaccinationUseCase rescheduleVaccinationUseCase;
    private final CancelVaccinationUseCase cancelVaccinationUseCase;
    private final DeleteVaccinationUseCase deleteVaccinationUseCase;

    @PostMapping("/pets/{petId}/vaccinations")
    public ResponseEntity<ApiResponse<CreateVaccinationResponse>> createVaccination(
            Authentication authentication,
            @PathVariable Long petId,
            @Valid @RequestBody CreateVaccinationRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createVaccinationUseCase.execute(userId, petId, request)));
    }

    @GetMapping("/pets/{petId}/vaccinations")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> getVaccinationList(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getVaccinationListUseCase.execute(userId, petId)));
    }

    // body省略時は当日を接種完了日とする
    @PatchMapping("/vaccinations/{vaccinationId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeVaccination(
            Authentication authentication,
            @PathVariable Long vaccinationId,
            @Valid @RequestBody(required = false) CompleteVaccinationRequest request
    ) {
        Long userId = extractUserId(authentication);
        completeVaccinationUseCase.execute(userId, vaccinationId, request != null ? request.completedDate() : null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/vaccinations/{vaccinationId}/reschedule")
    public ResponseEntity<ApiResponse<Void>> rescheduleVaccination(
            Authentication authentication,
            @PathVariable Long vaccinationId,
            @Valid @RequestBody RescheduleVaccinationRequest request
    ) {
        Long userId = extractUserId(authentication);
        rescheduleVaccinationUseCase.execute(userId, vaccinationId, request.scheduledDate());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/vaccinations/{vaccinationId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelVaccination(
            Authentication authentication,
            @PathVariable Long vaccinationId
    ) {
        Long userId = extractUserId(authentication);
        cancelVaccinationUseCase.execute(userId, vaccinationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/vaccinations/{vaccinationId}")
    public ResponseEntity<ApiResponse<Void>> deleteVaccination(
            Authentication authentication,
            @PathVariable Long vaccinationId
    ) {
        Long userId = extractUserId(authentication);
        deleteVaccinationUseCase.execute(userId, vaccinationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
