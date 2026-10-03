package koh.portfolio.springapi.presentation.healthrecord;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.HealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.UpdateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.usecase.CreateHealthRecordUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.DeleteHealthRecordUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.GetHealthRecordListUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.UpdateHealthRecordUseCase;
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
public class HealthRecordController {
    private final CreateHealthRecordUseCase createHealthRecordUseCase;
    private final GetHealthRecordListUseCase getHealthRecordListUseCase;
    private final UpdateHealthRecordUseCase updateHealthRecordUseCase;
    private final DeleteHealthRecordUseCase deleteHealthRecordUseCase;

    @PostMapping("/pets/{petId}/health-records")
    public ResponseEntity<ApiResponse<CreateHealthRecordResponse>> createHealthRecord(
            Authentication authentication,
            @PathVariable Long petId,
            @Valid @RequestBody CreateHealthRecordRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createHealthRecordUseCase.execute(userId, petId, request)));
    }

    @GetMapping("/pets/{petId}/health-records")
    public ResponseEntity<ApiResponse<List<HealthRecordResponse>>> getHealthRecordList(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getHealthRecordListUseCase.execute(userId, petId)));
    }

    @PatchMapping("/health-records/{healthRecordId}")
    public ResponseEntity<ApiResponse<Void>> updateHealthRecord(
            Authentication authentication,
            @PathVariable Long healthRecordId,
            @Valid @RequestBody UpdateHealthRecordRequest request
    ) {
        Long userId = extractUserId(authentication);
        updateHealthRecordUseCase.execute(userId, healthRecordId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/health-records/{healthRecordId}")
    public ResponseEntity<ApiResponse<Void>> deleteHealthRecord(
            Authentication authentication,
            @PathVariable Long healthRecordId
    ) {
        Long userId = extractUserId(authentication);
        deleteHealthRecordUseCase.execute(userId, healthRecordId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
