package koh.portfolio.springapi.presentation.hospital;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesResponse;
import koh.portfolio.springapi.application.hospital.usecase.BlockHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GenerateHospitalSchedulesUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UnblockHospitalScheduleUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalScheduleAdminController {
    private final CreateHospitalScheduleUseCase createHospitalScheduleUseCase;
    private final BlockHospitalScheduleUseCase blockHospitalScheduleUseCase;
    private final UnblockHospitalScheduleUseCase unblockHospitalScheduleUseCase;
    private final GenerateHospitalSchedulesUseCase generateHospitalSchedulesUseCase;

    @PostMapping("{hospitalId}/schedules")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateHospitalScheduleResponse>> createHospitalSchedule(
            Authentication authentication,
            @PathVariable Long hospitalId,
            @RequestBody @Valid CreateHospitalScheduleRequest request
    ) {
        CreateHospitalScheduleResponse createdHospitalScheduleId = createHospitalScheduleUseCase.execute(extractUserId(authentication), hospitalId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdHospitalScheduleId));
    }

    @PostMapping("/{hospitalId}/schedules/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<GenerateHospitalSchedulesResponse>> generateHospitalSchedules(
            Authentication authentication,
            @PathVariable Long hospitalId,
            @RequestBody @Valid GenerateHospitalSchedulesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                generateHospitalSchedulesUseCase.execute(extractUserId(authentication), hospitalId, request.month())));
    }

    @PatchMapping("/{hospitalId}/schedules/{scheduleId}/block")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> blockHospitalSchedule(
            Authentication authentication,
            @PathVariable Long hospitalId,
            @PathVariable Long scheduleId
    ) {
        blockHospitalScheduleUseCase.execute(extractUserId(authentication), hospitalId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{hospitalId}/schedules/{scheduleId}/unblock")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unblockHospitalSchedule(
            Authentication authentication,
            @PathVariable Long hospitalId,
            @PathVariable Long scheduleId
    ) {
        unblockHospitalScheduleUseCase.execute(extractUserId(authentication), hospitalId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
