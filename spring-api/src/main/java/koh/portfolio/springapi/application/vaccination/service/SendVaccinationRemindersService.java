package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.usecase.SendVaccinationRemindersUseCase;
import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.port.NotificationRepository;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SendVaccinationRemindersService implements SendVaccinationRemindersUseCase {
    // 08_State_Design §11-1: 接種予定日が7日以内
    static final int REMIND_DAYS_BEFORE = 7;

    private final VaccinationRepository vaccinationRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public int execute(LocalDate today) {
        List<VaccinationReminderTarget> targets =
                vaccinationRepository.findReminderTargets(today, today.plusDays(REMIND_DAYS_BEFORE));

        int sent = 0;
        for (VaccinationReminderTarget target : targets) {
            // 送信済みの目印を先に条件付きUPDATEで確保する。抽出後に予定が変わっていれば0件となり通知しない。
            // 行ロックで中止・再登録と直列化されるため、通知の予定日と送信済みの予定日は必ず一致する
            if (!vaccinationRepository.markReminded(target.vaccinationId(), target.scheduledDate())) {
                continue;
            }
            notificationRepository.save(Notification.forVaccination(
                    target.userId(),
                    target.vaccinationId(),
                    "%sの%s接種予定日（%s）が近づいています。".formatted(
                            target.petName(), target.vaccineName(), target.scheduledDate())
            ));
            sent++;
        }

        return sent;
    }
}
