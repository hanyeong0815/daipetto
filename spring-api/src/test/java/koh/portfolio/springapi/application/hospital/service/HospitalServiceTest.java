package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalDetailResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalSummary;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.UpdateHospitalRequest;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HospitalServiceTest {

    private HospitalRepository hospitalRepository;
    private HospitalService hospitalService;
    private GetHospitalDetailService getHospitalDetailService;
    private UpdateHospitalService updateHospitalService;
    private SuspendHospitalService suspendHospitalService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        hospitalService = new HospitalService(hospitalRepository);
        getHospitalDetailService = new GetHospitalDetailService(hospitalRepository);
        updateHospitalService = new UpdateHospitalService(hospitalRepository);
        suspendHospitalService = new SuspendHospitalService(hospitalRepository);
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("病院登録成功時、登録されたhospitalIdを返却する")
    void create_hospital_success() {
        // given
        CreateHospitalRequest request = new CreateHospitalRequest("Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678");

        LocalDateTime now = LocalDateTime.now();
        Hospital saved = new Hospital(1L, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);

        when(hospitalRepository.save(any(Hospital.class))).thenReturn(saved);

        // when
        CreateHospitalResponse response = hospitalService.execute(request);

        // then
        assertThat(response.hospitalId()).isEqualTo(1L);
        verify(hospitalRepository).save(any(Hospital.class));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("病院一覧取得成功時、keyword・areaをリポジトリに渡し一覧を返却する")
    void get_hospital_list_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Hospital> hospitals = List.of(
                new Hospital(1L, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                        HospitalStatus.ACTIVE, now, now, null)
        );

        when(hospitalRepository.search("Tokyo", "Shibuya")).thenReturn(hospitals);

        // when
        List<HospitalSummary> result = hospitalService.execute("Tokyo", "Shibuya");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Tokyo Animal Hospital");
        assertThat(result.get(0).address()).isEqualTo("Tokyo, Shibuya");
        verify(hospitalRepository).search("Tokyo", "Shibuya");
    }

    // -------------------------------------------------------------- getDetail

    @Test
    @DisplayName("病院詳細取得成功時、病院情報を返却する")
    void get_hospital_detail_success() {
        // given
        Long hospitalId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Hospital hospital = new Hospital(hospitalId, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));

        // when
        HospitalDetailResponse response = getHospitalDetailService.execute(hospitalId);

        // then
        assertThat(response.id()).isEqualTo(hospitalId);
        assertThat(response.name()).isEqualTo("Tokyo Animal Hospital");
        assertThat(response.status()).isEqualTo(HospitalStatus.ACTIVE);
    }

    @Test
    @DisplayName("存在しない病院の詳細取得時、HOSPITAL-001例外が発生する")
    void get_hospital_detail_fail_when_not_found() {
        // given
        Long hospitalId = 999L;
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getHospitalDetailService.execute(hospitalId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });
    }

    // ------------------------------------------------------------------ update

    @Test
    @DisplayName("病院情報更新成功時、更新された内容で保存される")
    void update_hospital_success() {
        // given
        Long hospitalId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Hospital hospital = new Hospital(hospitalId, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);
        UpdateHospitalRequest request = new UpdateHospitalRequest("Tokyo Animal Hospital 2nd", "Tokyo, Shinjuku", "03-9999-9999");

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateHospitalService.execute(hospitalId, request);

        // then
        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Tokyo Animal Hospital 2nd");
        assertThat(captor.getValue().getAddress()).isEqualTo("Tokyo, Shinjuku");
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("03-9999-9999");
    }

    @Test
    @DisplayName("存在しない病院更新時、HOSPITAL-001例外が発生する")
    void update_hospital_fail_when_not_found() {
        // given
        Long hospitalId = 999L;
        UpdateHospitalRequest request = new UpdateHospitalRequest("Tokyo Animal Hospital 2nd", "Tokyo, Shinjuku", null);
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateHospitalService.execute(hospitalId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalRepository, never()).save(any(Hospital.class));
    }

    // ----------------------------------------------------------------- suspend

    @Test
    @DisplayName("病院停止成功時、statusがSUSPENDEDで保存される")
    void suspend_hospital_success() {
        // given
        Long hospitalId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Hospital hospital = new Hospital(hospitalId, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        suspendHospitalService.execute(hospitalId);

        // then
        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(HospitalStatus.SUSPENDED);
    }

    @Test
    @DisplayName("存在しない病院停止時、HOSPITAL-001例外が発生する")
    void suspend_hospital_fail_when_not_found() {
        // given
        Long hospitalId = 999L;
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> suspendHospitalService.execute(hospitalId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalRepository, never()).save(any(Hospital.class));
    }
}
