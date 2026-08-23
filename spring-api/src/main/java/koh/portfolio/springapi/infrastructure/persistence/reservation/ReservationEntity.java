package koh.portfolio.springapi.infrastructure.persistence.reservation;

import jakarta.persistence.*;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long petId;
    private Long hospitalId;
    private Long scheduleId;
    private LocalDateTime reservationDatetime;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
