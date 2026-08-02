package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;
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
import org.mockito.ArgumentCaptor;

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
    private BlockHospitalScheduleService blockHospitalScheduleService;
    private UnblockHospitalScheduleService unblockHospitalScheduleService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        hospitalScheduleRepository = mock(HospitalScheduleRepository.class);
        hospitalScheduleService = new HospitalScheduleService(hospitalRepository, hospitalScheduleRepository);
        blockHospitalScheduleService = new BlockHospitalScheduleService(hospitalScheduleRepository);
        unblockHospitalScheduleService = new UnblockHospitalScheduleService(hospitalScheduleRepository);
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

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("予約枠登録成功時、登録されたscheduleIdを返却する")
    void create_hospital_schedule_success() {
        // given
        Long hospitalId = 1L;
        CreateHospitalScheduleRequest request = new CreateHospitalScheduleRequest(
                LocalDate.of(2026, 5, 20), LocalTime.of(10, 0), LocalTime.of(10, 30)
        );

        when(hospitalRepository.existsById(hospitalId)).thenReturn(true);
        when(hospitalScheduleRepository.save(any(HospitalSchedule.class)))
                .thenAnswer(invocation -> {
                    HospitalSchedule arg = invocation.getArgument(0);
                    return new HospitalSchedule(1L, arg.getHospitalId(), arg.getAvailableDate(),
                            arg.getStartTime(), arg.getEndTime(), arg.getStatus(), arg.getCreatedAt(), arg.getUpdatedAt());
                });

        // when
        CreateHospitalScheduleResponse response = hospitalScheduleService.execute(hospitalId, request);

        // then
        assertThat(response.scheduleId()).isEqualTo(1L);
        verify(hospitalScheduleRepository).save(any(HospitalSchedule.class));
    }

    @Test
    @DisplayName("存在しない病院への予約枠登録時、HOSPITAL-001例外が発生する")
    void create_hospital_schedule_fail_when_hospital_not_found() {
        // given
        Long hospitalId = 999L;
        CreateHospitalScheduleRequest request = new CreateHospitalScheduleRequest(
                LocalDate.of(2026, 5, 20), LocalTime.of(10, 0), LocalTime.of(10, 30)
        );

        when(hospitalRepository.existsById(hospitalId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> hospitalScheduleService.execute(hospitalId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).save(any(HospitalSchedule.class));
    }

    // ------------------------------------------------------------------ block

    @Test
    @DisplayName("予約枠BLOCKED設定成功時、statusがBLOCKEDで保存される")
    void block_hospital_schedule_success() {
        // given
        Long hospitalId = 1L;
        Long scheduleId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalSchedule schedule = new HospitalSchedule(scheduleId, hospitalId, LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, now, now);

        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(hospitalScheduleRepository.save(any(HospitalSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        blockHospitalScheduleService.execute(hospitalId, scheduleId);

        // then
        ArgumentCaptor<HospitalSchedule> captor = ArgumentCaptor.forClass(HospitalSchedule.class);
        verify(hospitalScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(HospitalScheduleStatus.BLOCKED);
    }

    @Test
    @DisplayName("存在しない予約枠のBLOCKED設定時、HOSPITAL-002例外が発生する")
    void block_hospital_schedule_fail_when_not_found() {
        // given
        Long hospitalId = 1L;
        Long scheduleId = 999L;
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> blockHospitalScheduleService.execute(hospitalId, scheduleId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).save(any(HospitalSchedule.class));
    }

    @Test
    @DisplayName("他病院の予約枠をBLOCKED設定しようとした時、HOSPITAL-002例外が発生する")
    void block_hospital_schedule_fail_when_hospital_mismatch() {
        // given
        Long hospitalId = 1L;
        Long anotherHospitalId = 2L;
        Long scheduleId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalSchedule schedule = new HospitalSchedule(scheduleId, anotherHospitalId, LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, now, now);

        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> blockHospitalScheduleService.execute(hospitalId, scheduleId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).save(any(HospitalSchedule.class));
    }

    // ---------------------------------------------------------------- unblock

    @Test
    @DisplayName("予約枠AVAILABLE設定成功時、statusがAVAILABLEで保存される")
    void unblock_hospital_schedule_success() {
        // given
        Long hospitalId = 1L;
        Long scheduleId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalSchedule schedule = new HospitalSchedule(scheduleId, hospitalId, LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.BLOCKED, now, now);

        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(hospitalScheduleRepository.save(any(HospitalSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        unblockHospitalScheduleService.execute(hospitalId, scheduleId);

        // then
        ArgumentCaptor<HospitalSchedule> captor = ArgumentCaptor.forClass(HospitalSchedule.class);
        verify(hospitalScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(HospitalScheduleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("存在しない予約枠のAVAILABLE設定時、HOSPITAL-002例外が発生する")
    void unblock_hospital_schedule_fail_when_not_found() {
        // given
        Long hospitalId = 1L;
        Long scheduleId = 999L;
        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> unblockHospitalScheduleService.execute(hospitalId, scheduleId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).save(any(HospitalSchedule.class));
    }

    @Test
    @DisplayName("他病院の予約枠をAVAILABLE設定しようとした時、HOSPITAL-002例外が発生する")
    void unblock_hospital_schedule_fail_when_hospital_mismatch() {
        // given
        Long hospitalId = 1L;
        Long anotherHospitalId = 2L;
        Long scheduleId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalSchedule schedule = new HospitalSchedule(scheduleId, anotherHospitalId, LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.BLOCKED, now, now);

        when(hospitalScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> unblockHospitalScheduleService.execute(hospitalId, scheduleId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND.code());
                });

        verify(hospitalScheduleRepository, never()).save(any(HospitalSchedule.class));
    }
}
