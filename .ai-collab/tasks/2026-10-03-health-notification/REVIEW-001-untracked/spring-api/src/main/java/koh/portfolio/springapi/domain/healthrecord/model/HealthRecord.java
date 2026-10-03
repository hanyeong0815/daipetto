package koh.portfolio.springapi.domain.healthrecord.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HealthRecord {
    private Long id;
    private Long petId;
    private BigDecimal weight;
    private String symptom;
    private String memo;
    private LocalDate recordedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static HealthRecord create(Long petId, BigDecimal weight, String symptom, String memo, LocalDate recordedDate) {
        LocalDateTime now = LocalDateTime.now();

        return new HealthRecord(null, petId, weight, symptom, memo, recordedDate, now, now, null);
    }

    public HealthRecord update(BigDecimal weight, String symptom, String memo, LocalDate recordedDate) {
        return new HealthRecord(this.id, this.petId, weight, symptom, memo, recordedDate,
                this.createdAt, LocalDateTime.now(), this.deletedAt);
    }
}
