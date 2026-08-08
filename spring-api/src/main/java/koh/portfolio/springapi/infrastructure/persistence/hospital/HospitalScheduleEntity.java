package koh.portfolio.springapi.infrastructure.persistence.hospital;

import jakarta.persistence.*;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "hospital_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HospitalScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long hospitalId;
    private LocalDate availableDate;
    private LocalTime startTime;
    private LocalTime endTime;
    @Enumerated(EnumType.STRING)
    private HospitalScheduleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
