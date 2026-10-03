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

    // PATCHの部分更新: null（未指定）の項目は既存値を維持する。
    // symptom / memo は空文字を送ると消去（NULL）できる。weight はPATCHでは消去できない
    public HealthRecord patch(BigDecimal weight, String symptom, String memo, LocalDate recordedDate) {
        return new HealthRecord(
                this.id,
                this.petId,
                weight != null ? weight : this.weight,
                mergeText(symptom, this.symptom),
                mergeText(memo, this.memo),
                recordedDate != null ? recordedDate : this.recordedDate,
                this.createdAt,
                LocalDateTime.now(),
                this.deletedAt
        );
    }

    private static String mergeText(String requested, String current) {
        if (requested == null) {
            return current;
        }

        return requested.isBlank() ? null : requested;
    }
}
