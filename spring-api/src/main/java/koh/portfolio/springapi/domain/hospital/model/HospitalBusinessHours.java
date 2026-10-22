package koh.portfolio.springapi.domain.hospital.model;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class HospitalBusinessHours {
    private static final int MIN_SLOT_MINUTES = 5;
    private static final int MAX_SLOT_MINUTES = 240;

    private Long id;
    private Long hospitalId;
    private DayOfWeek dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
    private Integer slotDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HospitalBusinessHours create(Long hospitalId, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, LocalTime breakStartTime, LocalTime breakEndTime, Integer slotDurationMinutes) {
        validateHours(openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes);
        LocalDateTime now = LocalDateTime.now();

        return new HospitalBusinessHours(null, hospitalId, dayOfWeek, openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes, now, now);
    }

    public HospitalBusinessHours update(LocalTime openTime, LocalTime closeTime, LocalTime breakStartTime, LocalTime breakEndTime, Integer slotDurationMinutes) {
        validateHours(openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes);

        return new HospitalBusinessHours(this.id, this.hospitalId, this.dayOfWeek, openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes, this.createdAt, LocalDateTime.now());
    }

    // 予約枠の自動生成（06_ERD §18）。開始〜休憩開始・休憩終了〜終了をそれぞれ枠の長さで区切ってAVAILABLE、
    // 休憩時間も同じ長さで区切ってBLOCKEDの枠にする。区間の端数（枠の長さに満たない残り）は作らない
    public List<HospitalSchedule> slotsOn(LocalDate date) {
        List<HospitalSchedule> slots = new ArrayList<>();
        // 登録時の検証より前に保存された不正な値でも無限ループや逆転した枠を作らない
        if (slotDurationMinutes == null || slotDurationMinutes <= 0 || !openTime.isBefore(closeTime)) {
            return slots;
        }

        // LocalTime の加算は 0 時をまたいで巻き戻るため、0 時からのナノ秒で計算する（秒以下も切り捨てない。REVIEW-001 R-02）
        long open = openTime.toNanoOfDay();
        long close = closeTime.toNanoOfDay();
        // 休憩は両端とも営業時間内に収める。営業時間外・営業時間と重ならない休憩（検証前の保存値）は休憩なしとして扱い、
        // 営業時間外に枠を作らない（REVIEW-001 R-01）
        boolean hasBreak = breakStartTime != null && breakEndTime != null;
        long breakStart = hasBreak ? clamp(breakStartTime.toNanoOfDay(), open, close) : close;
        long breakEnd = hasBreak ? clamp(breakEndTime.toNanoOfDay(), open, close) : close;
        if (breakStart >= breakEnd) {
            addSlots(slots, date, open, close, false);
            return slots;
        }

        addSlots(slots, date, open, breakStart, false);
        addSlots(slots, date, breakStart, breakEnd, true);
        addSlots(slots, date, breakEnd, close, false);
        return slots;
    }

    private void addSlots(List<HospitalSchedule> slots, LocalDate date, long from, long to, boolean blocked) {
        long duration = slotDurationMinutes * 60_000_000_000L;
        for (long start = from; start + duration <= to; start += duration) {
            HospitalSchedule slot = HospitalSchedule.create(
                    hospitalId, date, LocalTime.ofNanoOfDay(start), LocalTime.ofNanoOfDay(start + duration));
            slots.add(blocked ? slot.block() : slot);
        }
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    // 開始 < 終了、枠の長さ 5〜240 分、休憩は開始・終了の両方を指定し、営業時間内で 開始 < 終了
    private static void validateHours(LocalTime openTime, LocalTime closeTime, LocalTime breakStartTime, LocalTime breakEndTime, Integer slotDurationMinutes) {
        boolean noBreak = breakStartTime == null && breakEndTime == null;
        boolean validBreak = breakStartTime != null && breakEndTime != null
                && !breakStartTime.isBefore(openTime) && breakStartTime.isBefore(breakEndTime) && !breakEndTime.isAfter(closeTime);

        Preconditions.validate(
                openTime.isBefore(closeTime)
                        && slotDurationMinutes >= MIN_SLOT_MINUTES && slotDurationMinutes <= MAX_SLOT_MINUTES
                        && (noBreak || validBreak),
                HospitalErrorCode.INVALID_BUSINESS_HOURS
        );
    }
}
