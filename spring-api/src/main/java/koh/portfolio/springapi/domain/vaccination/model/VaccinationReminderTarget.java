package koh.portfolio.springapi.domain.vaccination.model;

import java.time.LocalDate;

// リマインダー通知の送信対象（通知先ユーザーはペットの飼い主）
public record VaccinationReminderTarget(
        Long vaccinationId,
        Long userId,
        String petName,
        String vaccineName,
        LocalDate scheduledDate
) {}
