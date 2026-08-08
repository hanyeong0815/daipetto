package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.HospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.UpdateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class HospitalBusinessHoursServiceTest {

    private HospitalRepository hospitalRepository;
    private HospitalBusinessHoursRepository hospitalBusinessHoursRepository;
    private HospitalBusinessHoursService hospitalBusinessHoursService;
    private UpdateHospitalBusinessHoursService updateHospitalBusinessHoursService;
    private DeleteHospitalBusinessHoursService deleteHospitalBusinessHoursService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        hospitalBusinessHoursRepository = mock(HospitalBusinessHoursRepository.class);
        hospitalBusinessHoursService = new HospitalBusinessHoursService(hospitalRepository, hospitalBusinessHoursRepository);
        updateHospitalBusinessHoursService = new UpdateHospitalBusinessHoursService(hospitalBusinessHoursRepository);
        deleteHospitalBusinessHoursService = new DeleteHospitalBusinessHoursService(hospitalBusinessHoursRepository);
    }

    // ------------------------------------------------------------------- list

    @Test
    @DisplayName("病院営業時間一覧取得成功時、一覧を返却する")
    void get_hospital_business_hours_list_success() {
        // given
        Long hospitalId = 1L;
        LocalDateTime now = LocalDateTime.now();
        when(hospitalRepository.existsById(hospitalId)).thenReturn(true);

        List<HospitalBusinessHours> businessHours = List.of(
                new HospitalBusinessHours(1L, hospitalId, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(18, 0),
                        LocalTime.of(12, 0), LocalTime.of(13, 0), 30, now, now)
        );
        when(hospitalBusinessHoursRepository.findByHospitalId(hospitalId)).thenReturn(businessHours);

        // when
        List<HospitalBusinessHoursResponse> result = hospitalBusinessHoursService.execute(hospitalId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).businessHoursId()).isEqualTo(1L);
        assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.get(0).openTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).closeTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("存在しない病院の営業時間一覧取得時、HOSPITAL-001例外が発生する")
    void get_hospital_business_hours_list_fail_when_hospital_not_found() {
        // given
        Long hospitalId = 999L;
        when(hospitalRepository.existsById(hospitalId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> hospitalBusinessHoursService.execute(hospitalId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).findByHospitalId(anyLong());
    }

    // ----------------------------------------------------------------- create

    @Test
    @DisplayName("営業時間登録成功時、登録されたbusinessHoursIdを返却する")
    void create_hospital_business_hours_success() {
        // given
        Long hospitalId = 1L;
        CreateHospitalBusinessHoursRequest request = new CreateHospitalBusinessHoursRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), 30
        );

        when(hospitalRepository.existsById(hospitalId)).thenReturn(true);
        when(hospitalBusinessHoursRepository.existsByHospitalIdAndDayOfWeek(hospitalId, DayOfWeek.MONDAY)).thenReturn(false);
        when(hospitalBusinessHoursRepository.save(any(HospitalBusinessHours.class)))
                .thenAnswer(invocation -> {
                    HospitalBusinessHours arg = invocation.getArgument(0);
                    return new HospitalBusinessHours(1L, arg.getHospitalId(), arg.getDayOfWeek(), arg.getOpenTime(),
                            arg.getCloseTime(), arg.getBreakStartTime(), arg.getBreakEndTime(),
                            arg.getSlotDurationMinutes(), arg.getCreatedAt(), arg.getUpdatedAt());
                });

        // when
        CreateHospitalBusinessHoursResponse response = hospitalBusinessHoursService.execute(hospitalId, request);

        // then
        assertThat(response.businessHoursId()).isEqualTo(1L);
        verify(hospitalBusinessHoursRepository).save(any(HospitalBusinessHours.class));
    }

    @Test
    @DisplayName("存在しない病院への営業時間登録時、HOSPITAL-001例外が発生する")
    void create_hospital_business_hours_fail_when_hospital_not_found() {
        // given
        Long hospitalId = 999L;
        CreateHospitalBusinessHoursRequest request = new CreateHospitalBusinessHoursRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30
        );

        when(hospitalRepository.existsById(hospitalId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> hospitalBusinessHoursService.execute(hospitalId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).save(any(HospitalBusinessHours.class));
    }

    @Test
    @DisplayName("既に登録されている曜日への営業時間登録時、HOSPITAL-004例外が発生する")
    void create_hospital_business_hours_fail_when_duplicated() {
        // given
        Long hospitalId = 1L;
        CreateHospitalBusinessHoursRequest request = new CreateHospitalBusinessHoursRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30
        );

        when(hospitalRepository.existsById(hospitalId)).thenReturn(true);
        when(hospitalBusinessHoursRepository.existsByHospitalIdAndDayOfWeek(hospitalId, DayOfWeek.MONDAY)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> hospitalBusinessHoursService.execute(hospitalId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_DUPLICATED.code());
                });

        verify(hospitalBusinessHoursRepository, never()).save(any(HospitalBusinessHours.class));
    }

    // ----------------------------------------------------------------- update

    @Test
    @DisplayName("営業時間更新成功時、変更内容が保存される")
    void update_hospital_business_hours_success() {
        // given
        Long hospitalId = 1L;
        Long businessHoursId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHours businessHours = new HospitalBusinessHours(businessHoursId, hospitalId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30, now, now);
        UpdateHospitalBusinessHoursRequest request = new UpdateHospitalBusinessHoursRequest(
                LocalTime.of(10, 0), LocalTime.of(19, 0), LocalTime.of(12, 0), LocalTime.of(13, 0), 20
        );

        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.of(businessHours));
        when(hospitalBusinessHoursRepository.save(any(HospitalBusinessHours.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateHospitalBusinessHoursService.execute(hospitalId, businessHoursId, request);

        // then
        ArgumentCaptor<HospitalBusinessHours> captor = ArgumentCaptor.forClass(HospitalBusinessHours.class);
        verify(hospitalBusinessHoursRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(captor.getValue().getCloseTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(captor.getValue().getSlotDurationMinutes()).isEqualTo(20);
        assertThat(captor.getValue().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("存在しない営業時間の更新時、HOSPITAL-003例外が発生する")
    void update_hospital_business_hours_fail_when_not_found() {
        // given
        Long hospitalId = 1L;
        Long businessHoursId = 999L;
        UpdateHospitalBusinessHoursRequest request = new UpdateHospitalBusinessHoursRequest(
                LocalTime.of(10, 0), LocalTime.of(19, 0), null, null, 20
        );

        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateHospitalBusinessHoursService.execute(hospitalId, businessHoursId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).save(any(HospitalBusinessHours.class));
    }

    @Test
    @DisplayName("他病院の営業時間を更新しようとした時、HOSPITAL-003例外が発生する")
    void update_hospital_business_hours_fail_when_hospital_mismatch() {
        // given
        Long hospitalId = 1L;
        Long anotherHospitalId = 2L;
        Long businessHoursId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHours businessHours = new HospitalBusinessHours(businessHoursId, anotherHospitalId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30, now, now);
        UpdateHospitalBusinessHoursRequest request = new UpdateHospitalBusinessHoursRequest(
                LocalTime.of(10, 0), LocalTime.of(19, 0), null, null, 20
        );

        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.of(businessHours));

        // when & then
        assertThatThrownBy(() -> updateHospitalBusinessHoursService.execute(hospitalId, businessHoursId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).save(any(HospitalBusinessHours.class));
    }

    // ----------------------------------------------------------------- delete

    @Test
    @DisplayName("営業時間削除成功時、リポジトリのdeleteByIdが呼ばれる")
    void delete_hospital_business_hours_success() {
        // given
        Long hospitalId = 1L;
        Long businessHoursId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHours businessHours = new HospitalBusinessHours(businessHoursId, hospitalId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30, now, now);

        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.of(businessHours));

        // when
        deleteHospitalBusinessHoursService.execute(hospitalId, businessHoursId);

        // then
        verify(hospitalBusinessHoursRepository).deleteById(businessHoursId);
    }

    @Test
    @DisplayName("存在しない営業時間の削除時、HOSPITAL-003例外が発生する")
    void delete_hospital_business_hours_fail_when_not_found() {
        // given
        Long hospitalId = 1L;
        Long businessHoursId = 999L;
        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteHospitalBusinessHoursService.execute(hospitalId, businessHoursId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("他病院の営業時間を削除しようとした時、HOSPITAL-003例外が発生する")
    void delete_hospital_business_hours_fail_when_hospital_mismatch() {
        // given
        Long hospitalId = 1L;
        Long anotherHospitalId = 2L;
        Long businessHoursId = 10L;
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHours businessHours = new HospitalBusinessHours(businessHoursId, anotherHospitalId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30, now, now);

        when(hospitalBusinessHoursRepository.findById(businessHoursId)).thenReturn(Optional.of(businessHours));

        // when & then
        assertThatThrownBy(() -> deleteHospitalBusinessHoursService.execute(hospitalId, businessHoursId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND.code());
                });

        verify(hospitalBusinessHoursRepository, never()).deleteById(anyLong());
    }
}
