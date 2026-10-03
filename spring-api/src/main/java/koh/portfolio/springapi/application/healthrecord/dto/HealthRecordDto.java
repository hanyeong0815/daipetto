package koh.portfolio.springapi.application.healthrecord.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordDto() {
    public record CreateHealthRecordRequest(
            @PositiveOrZero
            @Digits(integer = 3, fraction = 2)
            BigDecimal weight,
            @Size(max = 255)
            String symptom,
            String memo,
            // 省略時は当日。過去の記録を後から登録する場合のみ指定する
            LocalDate recordedDate
    ) {}

    @Builder
    public record CreateHealthRecordResponse(
            Long healthRecordId
    ) {}

    public record UpdateHealthRecordRequest(
            @PositiveOrZero
            @Digits(integer = 3, fraction = 2)
            BigDecimal weight,
            @Size(max = 255)
            String symptom,
            String memo,
            LocalDate recordedDate
    ) {}

    @Builder
    public record HealthRecordResponse(
            Long healthRecordId,
            BigDecimal weight,
            String symptom,
            String memo,
            LocalDate recordedDate
    ) {}
}
