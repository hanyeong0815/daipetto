package koh.portfolio.springapi.domain.hospital.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class HospitalBusinessHours {
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
        LocalDateTime now = LocalDateTime.now();

        return new HospitalBusinessHours(null, hospitalId, dayOfWeek, openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes, now, now);
    }

    public HospitalBusinessHours update(LocalTime openTime, LocalTime closeTime, LocalTime breakStartTime, LocalTime breakEndTime, Integer slotDurationMinutes) {
        return new HospitalBusinessHours(this.id, this.hospitalId, this.dayOfWeek, openTime, closeTime, breakStartTime, breakEndTime, slotDurationMinutes, this.createdAt, LocalDateTime.now());
    }
}
