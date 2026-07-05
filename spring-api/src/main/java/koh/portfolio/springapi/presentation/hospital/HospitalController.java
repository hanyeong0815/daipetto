package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalDetailResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalSummary;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalDetailUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalListUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {
    private final GetHospitalListUseCase getHospitalListUseCase;
    private final GetHospitalDetailUseCase getHospitalDetailUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HospitalSummary>>> getHospitalList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area
    ) {
        List<HospitalSummary> response = getHospitalListUseCase.execute(keyword, area);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{hospitalId}")
    public ResponseEntity<ApiResponse<HospitalDetailResponse>> getHospitalDetail(@PathVariable Long hospitalId) {
        HospitalDetailResponse response = getHospitalDetailUseCase.execute(hospitalId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
