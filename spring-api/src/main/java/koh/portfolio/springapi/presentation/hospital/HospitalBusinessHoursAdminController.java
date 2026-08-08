package koh.portfolio.springapi.presentation.hospital;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.UpdateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.application.hospital.usecase.DeleteHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalBusinessHoursAdminController {
    private final CreateHospitalBusinessHoursUseCase createHospitalBusinessHoursUseCase;
    private final UpdateHospitalBusinessHoursUseCase updateHospitalBusinessHoursUseCase;
    private final DeleteHospitalBusinessHoursUseCase deleteHospitalBusinessHoursUseCase;

    @PostMapping("/{hospitalId}/business-hours")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateHospitalBusinessHoursResponse>> createHospitalBusinessHours(
            @PathVariable Long hospitalId,
            @RequestBody @Valid CreateHospitalBusinessHoursRequest request
    ) {
        CreateHospitalBusinessHoursResponse response = createHospitalBusinessHoursUseCase.execute(hospitalId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/{hospitalId}/business-hours/{businessHoursId}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateHospitalBusinessHours(
            @PathVariable Long hospitalId,
            @PathVariable Long businessHoursId,
            @RequestBody @Valid UpdateHospitalBusinessHoursRequest request
    ) {
        updateHospitalBusinessHoursUseCase.execute(hospitalId, businessHoursId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{hospitalId}/business-hours/{businessHoursId}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteHospitalBusinessHours(
            @PathVariable Long hospitalId,
            @PathVariable Long businessHoursId
    ) {
        deleteHospitalBusinessHoursUseCase.execute(hospitalId, businessHoursId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
