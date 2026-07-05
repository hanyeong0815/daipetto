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
}
