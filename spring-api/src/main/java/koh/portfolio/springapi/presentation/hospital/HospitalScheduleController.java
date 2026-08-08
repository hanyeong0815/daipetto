package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.HospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalScheduleListUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HospitalScheduleController {
    private final GetHospitalScheduleListUseCase getHospitalScheduleListUseCase;

    @GetMapping("/api/v1/hospitals/{hospitalId}/schedules")
    public ResponseEntity<ApiResponse<List<HospitalScheduleResponse>>> getHospitalScheduleList(
            @PathVariable Long hospitalId
    ) {
        List<HospitalScheduleResponse> response = getHospitalScheduleListUseCase.execute(hospitalId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
