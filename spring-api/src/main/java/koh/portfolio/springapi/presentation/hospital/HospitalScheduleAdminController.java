package koh.portfolio.springapi.presentation.hospital;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.usecase.BlockHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UnblockHospitalScheduleUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalScheduleAdminController {
    private final CreateHospitalScheduleUseCase createHospitalScheduleUseCase;
    private final BlockHospitalScheduleUseCase blockHospitalScheduleUseCase;
    private final UnblockHospitalScheduleUseCase unblockHospitalScheduleUseCase;

    @PostMapping("{hospitalId}/schedules")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateHospitalScheduleResponse>> createHospitalSchedule(
            @PathVariable Long hospitalId,
            @RequestBody @Valid CreateHospitalScheduleRequest request
    ) {
        CreateHospitalScheduleResponse createdHospitalScheduleId = createHospitalScheduleUseCase.execute(hospitalId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdHospitalScheduleId));
    }

    @PatchMapping("/{hospitalId}/schedules/{scheduleId}/block")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> blockHospitalSchedule(
            @PathVariable Long hospitalId,
            @PathVariable Long scheduleId
    ) {
        blockHospitalScheduleUseCase.execute(hospitalId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{hospitalId}/schedules/{scheduleId}/unblock")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unblockHospitalSchedule(
            @PathVariable Long hospitalId,
            @PathVariable Long scheduleId
    ) {
        unblockHospitalScheduleUseCase.execute(hospitalId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
