package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.HospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalBusinessHoursListUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HospitalBusinessHoursController {
    private final GetHospitalBusinessHoursListUseCase getHospitalBusinessHoursListUseCase;

    @GetMapping("/api/v1/hospitals/{hospitalId}/business-hours")
    public ResponseEntity<ApiResponse<List<HospitalBusinessHoursResponse>>> getHospitalBusinessHoursList(
            @PathVariable Long hospitalId
    ) {
        List<HospitalBusinessHoursResponse> response = getHospitalBusinessHoursListUseCase.execute(hospitalId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
