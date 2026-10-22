package koh.portfolio.springapi.presentation.hospital;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.AssignHospitalAdminRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.HospitalAdminResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.ManagedHospitalResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.usecase.AssignHospitalAdminUseCase;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalAdminsUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetManagedHospitalsUseCase;
import koh.portfolio.springapi.application.hospital.usecase.SuspendHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UnassignHospitalAdminUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalAdminController {
    private final CreateHospitalUseCase createHospitalUseCase;
    private final UpdateHospitalUseCase updateHospitalUseCase;
    private final SuspendHospitalUseCase suspendHospitalUseCase;
    private final GetManagedHospitalsUseCase getManagedHospitalsUseCase;
    private final GetHospitalAdminsUseCase getHospitalAdminsUseCase;
    private final AssignHospitalAdminUseCase assignHospitalAdminUseCase;
    private final UnassignHospitalAdminUseCase unassignHospitalAdminUseCase;

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

    // 管理画面で操作できる病院（システム管理者は全病院、病院管理者は紐付いた病院）
    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ManagedHospitalResponse>>> getManagedHospitals(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(getManagedHospitalsUseCase.execute(extractUserId(authentication))));
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
            Authentication authentication,
            @PathVariable Long hospitalId,
            @Valid @RequestBody HospitalDto.UpdateHospitalRequest request
    ) {
        updateHospitalUseCase.execute(extractUserId(authentication), hospitalId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ---- 病院管理者の紐付け（システム管理者のみ、06_ERD §11 hospital_admins）

    @GetMapping("/{hospitalId}/admins")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<HospitalAdminResponse>>> getHospitalAdmins(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(ApiResponse.success(getHospitalAdminsUseCase.execute(hospitalId)));
    }

    @PostMapping("/{hospitalId}/admins")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignHospitalAdmin(
            @PathVariable Long hospitalId,
            @Valid @RequestBody AssignHospitalAdminRequest request
    ) {
        assignHospitalAdminUseCase.execute(hospitalId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @DeleteMapping("/{hospitalId}/admins/{userId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unassignHospitalAdmin(
            @PathVariable Long hospitalId,
            @PathVariable Long userId
    ) {
        unassignHospitalAdminUseCase.execute(hospitalId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
