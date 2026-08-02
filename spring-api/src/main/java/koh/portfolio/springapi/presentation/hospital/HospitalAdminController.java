package koh.portfolio.springapi.presentation.hospital;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.SuspendHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalAdminController {
    private final CreateHospitalUseCase createHospitalUseCase;
    private final UpdateHospitalUseCase updateHospitalUseCase;
    private final SuspendHospitalUseCase suspendHospitalUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateHospitalResponse>> createHospital(
            @Valid @RequestBody CreateHospitalRequest request
    ) {
        CreateHospitalResponse response = createHospitalUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/{hospitalId}/suspend")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendHospital(
            @PathVariable Long hospitalId
    ) {
        suspendHospitalUseCase.execute(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{hospitalId}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateHospital(
            @PathVariable Long hospitalId,
            @Valid @RequestBody HospitalDto.UpdateHospitalRequest request
    ) {
        updateHospitalUseCase.execute(hospitalId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
