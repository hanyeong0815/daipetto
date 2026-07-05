package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.HospitalScheduleResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class HospitalScheduleServiceTest {

    private HospitalRepository hospitalRepository;
    private HospitalScheduleRepository hospitalScheduleRepository;
    private HospitalScheduleService hospitalScheduleService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        hospitalScheduleRepository = mock(HospitalScheduleRepository.class);
        hospitalScheduleService = new HospitalScheduleService(hospitalRepository, hospitalScheduleRepository);
    }

    @Test
    @DisplayName("病院予約枠一覧取得成功時、一覧を返却する")
    void get_hospital_schedule_list_success() {
        // given
        Long hospitalId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Hospital hospital = new Hospital(hospitalId, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));

        List<HospitalSchedule> schedules = List.of(
                new HospitalSchedule(1L, hospitalId, LocalDate.of(2026, 5, 20),
                        LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, now, now)
        );
        when(hospitalScheduleRepository.findAllByHospitalId(hospitalId)).thenReturn(schedules);

        // when
        List<HospitalScheduleResponse> result = hospitalScheduleService.execute(hospitalId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scheduleId()).isEqualTo(1L);
        assertThat(result.get(0).availableDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(result.get(0).status()).isEqualTo(HospitalScheduleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("存在しない病院の予約枠一覧取得時、HOSPITAL-001例外が発生する")
    void get_hospital_schedule_list_fail_when_hospital_not_found() {
        // given
        Long hospitalId = 999L;
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hospitalScheduleService.execute(hospitalId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).findAllByHospitalId(anyLong());
    }
}
