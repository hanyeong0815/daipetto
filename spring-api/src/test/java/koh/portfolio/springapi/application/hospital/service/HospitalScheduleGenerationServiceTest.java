package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;
import koh.portfolio.springapi.common.time.ServerTime;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class HospitalScheduleGenerationServiceTest {

    private final HospitalRepository hospitalRepository = mock(HospitalRepository.class);
    private final HospitalBusinessHoursRepository hospitalBusinessHoursRepository = mock(HospitalBusinessHoursRepository.class);
    private final HospitalScheduleRepository hospitalScheduleRepository = mock(HospitalScheduleRepository.class);
    private final HospitalAccessChecker hospitalAccessChecker = mock(HospitalAccessChecker.class);
    private final HospitalScheduleGenerator generator =
            new HospitalScheduleGenerator(hospitalRepository, hospitalBusinessHoursRepository, hospitalScheduleRepository);

    // 2026-11: 2日・9日・16日・23日・30日が月曜、4日・11日・18日・25日が水曜
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    private Hospital hospital(Long id, HospitalStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Hospital(id, "病院" + id, "東京都", "03", status, now, now, null);
    }

    private HospitalBusinessHours hours(DayOfWeek day, String open, String close, String breakStart, String breakEnd, int slot) {
        return HospitalBusinessHours.create(1L, day, LocalTime.parse(open), LocalTime.parse(close),
                breakStart == null ? null : LocalTime.parse(breakStart), breakEnd == null ? null : LocalTime.parse(breakEnd), slot);
    }

    private void assertErrorCode(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code()).isEqualTo(errorCode.code()));
    }

    @SuppressWarnings("unchecked")
    private List<HospitalSchedule> savedSlots() {
        ArgumentCaptor<List<HospitalSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(hospitalScheduleRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------- generator

    @Test
    @DisplayName("病院行を排他取得してから、営業時間のある曜日だけ枠を作る。既に枠がある日は飛ばす")
    void generate_slots_for_open_days_and_skip_existing_dates() {
        // given: 月曜 9:00〜12:00（休憩 10:00〜10:30、30分枠）、水曜 14:00〜15:00（60分枠）。11/9（月）は手動の枠あり
        when(hospitalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hospital(1L, HospitalStatus.ACTIVE)));
        when(hospitalBusinessHoursRepository.findByHospitalId(1L)).thenReturn(List.of(
                hours(DayOfWeek.MONDAY, "09:00", "12:00", "10:00", "10:30", 30),
                hours(DayOfWeek.WEDNESDAY, "14:00", "15:00", null, null, 60)));
        when(hospitalScheduleRepository.findDatesWithSchedules(1L, FROM, TO)).thenReturn(List.of(LocalDate.of(2026, 11, 9)));

        // when
        GenerateHospitalSchedulesResponse result = generator.generate(1L, FROM, TO);

        // then: 月曜 4日 × 6枠 + 水曜 4日 × 1枠
        assertThat(result).isEqualTo(new GenerateHospitalSchedulesResponse(28, 8, 1));
        List<HospitalSchedule> slots = savedSlots();
        assertThat(slots).hasSize(28);
        assertThat(slots).noneMatch(s -> s.getAvailableDate().equals(LocalDate.of(2026, 11, 9)));
        assertThat(slots).filteredOn(s -> s.getAvailableDate().equals(LocalDate.of(2026, 11, 2)))
                .extracting(s -> s.getStartTime() + " " + s.getStatus())
                .containsExactly("09:00 AVAILABLE", "09:30 AVAILABLE", "10:00 BLOCKED", "10:30 AVAILABLE", "11:00 AVAILABLE", "11:30 AVAILABLE");
        assertThat(slots).allMatch(s -> s.getHospitalId().equals(1L));
        assertThat(slots).filteredOn(s -> s.getStatus() == HospitalScheduleStatus.BLOCKED).hasSize(4);

        InOrder order = inOrder(hospitalRepository, hospitalScheduleRepository);
        order.verify(hospitalRepository).findByIdForUpdate(1L);
        order.verify(hospitalScheduleRepository).findDatesWithSchedules(1L, FROM, TO);
        order.verify(hospitalScheduleRepository).saveAll(any());
    }

    @Test
    @DisplayName("営業時間が未登録なら枠を作らない。存在しない病院はHOSPITAL-001で何も保存しない")
    void generate_nothing_without_hours_and_fail_for_missing_hospital() {
        // given
        when(hospitalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hospital(1L, HospitalStatus.ACTIVE)));
        when(hospitalBusinessHoursRepository.findByHospitalId(1L)).thenReturn(List.of());
        when(hospitalRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        // when & then
        assertThat(generator.generate(1L, FROM, TO)).isEqualTo(new GenerateHospitalSchedulesResponse(0, 0, 0));
        assertThat(savedSlots()).isEmpty();
        assertErrorCode(() -> generator.generate(2L, FROM, TO), HospitalErrorCode.HOSPITAL_NOT_FOUND);
        verify(hospitalBusinessHoursRepository, never()).findByHospitalId(2L);
    }

    // ---------------------------------------------------- manual generation

    private final HospitalScheduleGenerator mockGenerator = mock(HospitalScheduleGenerator.class);
    private final HospitalScheduleGenerationService service =
            new HospitalScheduleGenerationService(hospitalAccessChecker, hospitalRepository, mockGenerator);

    @Test
    @DisplayName("手動生成: 所属確認の後、翌月以降は1日から月末、今月は今日（JST）から月末で生成する")
    void manual_generation_ranges() {
        // given
        YearMonth current = YearMonth.now(ServerTime.ZONE_ID);
        LocalDate today = LocalDate.now(ServerTime.ZONE_ID);
        YearMonth next = current.plusMonths(1);
        GenerateHospitalSchedulesResponse response = new GenerateHospitalSchedulesResponse(10, 5, 0);
        when(mockGenerator.generate(anyLong(), any(), any())).thenReturn(response);

        // when
        GenerateHospitalSchedulesResponse result = service.execute(99L, 1L, next);
        service.execute(99L, 1L, current);
        service.execute(99L, 1L, current.plusMonths(12));

        // then
        assertThat(result).isEqualTo(response);
        InOrder order = inOrder(hospitalAccessChecker, mockGenerator);
        order.verify(hospitalAccessChecker).checkManageable(99L, 1L);
        order.verify(mockGenerator).generate(1L, next.atDay(1), next.atEndOfMonth());
        verify(mockGenerator).generate(1L, today, current.atEndOfMonth());
        verify(mockGenerator).generate(1L, current.plusMonths(12).atDay(1), current.plusMonths(12).atEndOfMonth());
    }

    @Test
    @DisplayName("手動生成: 過去の月・13か月以上先はHOSPITAL-010、管理対象外はHOSPITAL-005で生成しない")
    void manual_generation_rejects_out_of_range_month_and_unmanaged_hospital() {
        // given
        YearMonth current = YearMonth.now(ServerTime.ZONE_ID);
        doThrow(HospitalErrorCode.NOT_HOSPITAL_MANAGER.defaultException()).when(hospitalAccessChecker).checkManageable(99L, 2L);

        // when & then
        assertErrorCode(() -> service.execute(99L, 1L, current.minusMonths(1)), HospitalErrorCode.INVALID_GENERATION_MONTH);
        assertErrorCode(() -> service.execute(99L, 1L, current.plusMonths(13)), HospitalErrorCode.INVALID_GENERATION_MONTH);
        assertErrorCode(() -> service.execute(99L, 2L, current.plusMonths(1)), HospitalErrorCode.NOT_HOSPITAL_MANAGER);
        verifyNoInteractions(mockGenerator);
    }

    // ------------------------------------------------------ monthly batch

    @Test
    @DisplayName("月次バッチ: 営業中の病院だけを月初〜月末で生成し、1病院の失敗で他を止めず作成数を合計する")
    void monthly_generation_covers_active_hospitals_and_continues_after_failure() {
        // given
        YearMonth month = YearMonth.of(2026, 12);
        when(hospitalRepository.findAllForManagement()).thenReturn(List.of(
                hospital(1L, HospitalStatus.ACTIVE), hospital(2L, HospitalStatus.SUSPENDED),
                hospital(3L, HospitalStatus.ACTIVE), hospital(4L, HospitalStatus.ACTIVE)));
        when(mockGenerator.generate(1L, month.atDay(1), month.atEndOfMonth())).thenReturn(new GenerateHospitalSchedulesResponse(40, 20, 0));
        when(mockGenerator.generate(3L, month.atDay(1), month.atEndOfMonth())).thenThrow(new IllegalStateException("db down"));
        when(mockGenerator.generate(4L, month.atDay(1), month.atEndOfMonth())).thenReturn(new GenerateHospitalSchedulesResponse(5, 5, 2));

        // when
        int created = service.execute(month);

        // then
        assertThat(created).isEqualTo(45);
        verify(mockGenerator, never()).generate(eq(2L), any(), any());
        verify(mockGenerator).generate(4L, month.atDay(1), month.atEndOfMonth());
        verifyNoInteractions(hospitalAccessChecker);
    }
}
