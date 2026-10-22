package koh.portfolio.springapi.infrastructure.scheduler;

import koh.portfolio.springapi.application.hospital.usecase.GenerateMonthlyHospitalSchedulesUseCase;
import koh.portfolio.springapi.common.time.ServerTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalScheduleGenerationScheduler {
    private final GenerateMonthlyHospitalSchedulesUseCase generateMonthlyHospitalSchedulesUseCase;

    // 04_System_Architecture §14: 毎月1日 00:00（JST）に翌月分の予約枠を生成する
    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Tokyo")
    public void generateNextMonth() {
        YearMonth month = YearMonth.now(ServerTime.ZONE_ID).plusMonths(1);
        int created = generateMonthlyHospitalSchedulesUseCase.execute(month);
        log.info("{}の予約枠を{}件自動生成しました", month, created);
    }
}
