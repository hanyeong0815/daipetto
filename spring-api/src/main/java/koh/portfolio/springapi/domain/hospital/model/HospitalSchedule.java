package koh.portfolio.springapi.domain.hospital.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class HospitalSchedule {
    private Long id;
    private Long hospitalId;
    private LocalDate availableDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private HospitalScheduleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HospitalSchedule create(Long hospitalId, LocalDate availableDate, LocalTime startTime, LocalTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        return new HospitalSchedule(null, hospitalId, availableDate, startTime, endTime, HospitalScheduleStatus.AVAILABLE, now, now);
    }

    public HospitalSchedule block() {
        LocalDateTime now = LocalDateTime.now();

        return new HospitalSchedule(this.id, this.hospitalId, this.availableDate, this.startTime, this.endTime, HospitalScheduleStatus.BLOCKED, this.createdAt, now);
    }

    public HospitalSchedule makeAvailable() {
        LocalDateTime now = LocalDateTime.now();

        return new HospitalSchedule(this.id, this.hospitalId, this.availableDate, this.startTime, this.endTime, HospitalScheduleStatus.AVAILABLE, this.createdAt, now);
    }
}
