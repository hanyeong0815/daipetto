package koh.portfolio.springapi.domain.hospital.model;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HospitalBusinessHoursTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    private static LocalTime t(String hhmm) {
        return LocalTime.parse(hhmm);
    }

    // 検証を通さずに作る（検証の追加前に保存された値を想定）
    private static HospitalBusinessHours stored(String open, String close, String breakStart, String breakEnd, int slot) {
        LocalDateTime now = LocalDateTime.now();
        return new HospitalBusinessHours(1L, 7L, DayOfWeek.MONDAY, t(open), t(close),
                breakStart == null ? null : t(breakStart), breakEnd == null ? null : t(breakEnd), slot, now, now);
    }

    private static List<String> describe(List<HospitalSchedule> slots) {
        return slots.stream().map(s -> s.getStartTime() + "-" + s.getEndTime() + (s.getStatus() == HospitalScheduleStatus.BLOCKED ? " B" : "")).toList();
    }

    private static void assertInvalid(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HospitalErrorCode.INVALID_BUSINESS_HOURS.code()));
    }

    @Test
    @DisplayName("営業開始〜休憩開始・休憩終了〜営業終了をAVAILABLE、休憩をBLOCKEDの枠に区切る")
    void slots_split_by_break() {
        List<HospitalSchedule> slots = stored("09:00", "15:00", "12:00", "13:00", 60).slotsOn(DAY);

        assertThat(describe(slots)).containsExactly(
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00 B", "13:00-14:00", "14:00-15:00");
        assertThat(slots).allSatisfy(s -> {
            assertThat(s.getHospitalId()).isEqualTo(7L);
            assertThat(s.getAvailableDate()).isEqualTo(DAY);
            assertThat(s.getId()).isNull();
        });
    }

    @Test
    @DisplayName("区間ごとに区切り、枠の長さに満たない残りは作らない")
    void slots_drop_remainder_per_segment() {
        List<HospitalSchedule> slots = stored("09:00", "13:00", "11:00", "11:30", 45).slotsOn(DAY);

        // 09:00〜11:00 は 2 枠（10:30〜11:15 は休憩にかかるため作らない）、休憩 30 分は 45 分に満たない
        assertThat(describe(slots)).containsExactly("09:00-09:45", "09:45-10:30", "11:30-12:15", "12:15-13:00");
    }

    @Test
    @DisplayName("0時をまたぐ枠は作らない（LocalTimeの巻き戻りで無限ループしない）")
    void slots_do_not_wrap_midnight() {
        assertThat(describe(stored("23:00", "23:59", null, null, 30).slotsOn(DAY))).containsExactly("23:00-23:30");
    }

    @Test
    @DisplayName("検証前に保存された営業時間外・一部だけ重なる・片方だけの休憩でも、営業時間外に枠を作らない（REVIEW-001 R-01）")
    void slots_stay_within_opening_hours_for_invalid_stored_breaks() {
        // 営業時間の後・前の休憩は休憩なしとして扱う
        assertThat(describe(stored("09:00", "11:00", "19:00", "20:00", 60).slotsOn(DAY))).containsExactly("09:00-10:00", "10:00-11:00");
        assertThat(describe(stored("09:00", "11:00", "07:00", "08:00", 60).slotsOn(DAY))).containsExactly("09:00-10:00", "10:00-11:00");
        // 一部だけ重なる休憩は営業時間内の部分だけを休憩にする
        assertThat(describe(stored("09:00", "11:00", "08:00", "10:00", 60).slotsOn(DAY))).containsExactly("09:00-10:00 B", "10:00-11:00");
        assertThat(describe(stored("09:00", "11:00", "10:00", "12:00", 60).slotsOn(DAY))).containsExactly("09:00-10:00", "10:00-11:00 B");
        // 片方だけの休憩は休憩なし
        assertThat(describe(stored("09:00", "11:00", "10:00", null, 60).slotsOn(DAY))).containsExactly("09:00-10:00", "10:00-11:00");
    }

    @Test
    @DisplayName("秒を含む営業時間・休憩の境界を切り捨てず、その時刻から区切る（REVIEW-001 R-02）")
    void slots_keep_second_precision() {
        List<HospitalSchedule> slots = stored("09:00:30", "11:00:30", "10:00:30", "10:30:30", 30).slotsOn(DAY);

        assertThat(describe(slots)).containsExactly("09:00:30-09:30:30", "09:30:30-10:00:30", "10:00:30-10:30:30 B", "10:30:30-11:00:30");
    }

    @Test
    @DisplayName("検証前に保存された不正な値（枠0分・開始>=終了）では枠を作らない")
    void slots_empty_for_invalid_stored_values() {
        assertThat(stored("09:00", "18:00", null, null, 0).slotsOn(DAY)).isEmpty();
        assertThat(stored("18:00", "09:00", null, null, 30).slotsOn(DAY)).isEmpty();
    }

    @Test
    @DisplayName("登録・更新の検証: 開始<終了、枠5〜240分、休憩は両方指定かつ営業時間内（それ以外HOSPITAL-009）")
    void create_and_update_validate_hours() {
        assertThatCode(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), t("12:00"), t("13:00"), 30))
                .doesNotThrowAnyException();
        assertThatCode(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), null, null, 5))
                .doesNotThrowAnyException();

        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("18:00"), t("09:00"), null, null, 30));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("09:00"), null, null, 30));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), null, null, 4));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), null, null, 241));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), t("12:00"), null, 30));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), t("13:00"), t("12:00"), 30));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), t("08:00"), t("10:00"), 30));
        assertInvalid(() -> HospitalBusinessHours.create(1L, DayOfWeek.MONDAY, t("09:00"), t("18:00"), t("17:00"), t("19:00"), 30));

        assertInvalid(() -> stored("09:00", "18:00", null, null, 30).update(t("10:00"), t("09:00"), null, null, 30));
    }
}
