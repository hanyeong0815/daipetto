package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import jakarta.persistence.*;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "vaccinations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class VaccinationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long petId;
    private String vaccineName;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private LocalDate remindedDate;
    @Enumerated(EnumType.STRING)
    private VaccinationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
