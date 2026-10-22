package koh.portfolio.springapi.infrastructure.scheduler;

import koh.portfolio.springapi.application.vaccination.usecase.SendVaccinationRemindersUseCase;
import koh.portfolio.springapi.common.time.ServerTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class VaccinationReminderScheduler {
    private final SendVaccinationRemindersUseCase sendVaccinationRemindersUseCase;

    // 08_State_Design §11-1: 毎日 09:00（JST）
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tokyo")
    public void remind() {
        int created = sendVaccinationRemindersUseCase.execute(LocalDate.now(ServerTime.ZONE_ID));
        log.info("ワクチン接種リマインダー通知を{}件作成しました", created);
    }
}
