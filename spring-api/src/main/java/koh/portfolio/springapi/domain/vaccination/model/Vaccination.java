package koh.portfolio.springapi.domain.vaccination.model;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Vaccination {
    private Long id;
    private Long petId;
    private String vaccineName;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    // リマインダー送信済みの接種予定日（未送信ならnull）。予定日ごとに1回だけ通知するための目印
    private LocalDate remindedDate;
    private VaccinationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Vaccination create(Long petId, String vaccineName, LocalDate scheduledDate) {
        LocalDateTime now = LocalDateTime.now();

        return new Vaccination(null, petId, vaccineName, scheduledDate, null, null,
                VaccinationStatus.SCHEDULED, now, now, null);
    }

    public Vaccination complete(LocalDate completedDate) {
        Preconditions.validate(this.status == VaccinationStatus.SCHEDULED, VaccinationErrorCode.INVALID_STATE_TRANSITION);

        return new Vaccination(this.id, this.petId, this.vaccineName, this.scheduledDate, completedDate, this.remindedDate,
                VaccinationStatus.COMPLETED, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Vaccination cancel() {
        Preconditions.validate(this.status == VaccinationStatus.SCHEDULED, VaccinationErrorCode.INVALID_STATE_TRANSITION);

        return new Vaccination(this.id, this.petId, this.vaccineName, this.scheduledDate, null, this.remindedDate,
                VaccinationStatus.CANCELLED, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    // 08_State_Design §7「再登録」: 中止した予定を新しい接種予定日で SCHEDULED に戻す。
    // 送信済みの目印も消すため、新しい予定日で改めてリマインダー対象になる
    public Vaccination reschedule(LocalDate scheduledDate) {
        Preconditions.validate(this.status == VaccinationStatus.CANCELLED, VaccinationErrorCode.INVALID_STATE_TRANSITION);

        return new Vaccination(this.id, this.petId, this.vaccineName, scheduledDate, null, null,
                VaccinationStatus.SCHEDULED, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }
}
