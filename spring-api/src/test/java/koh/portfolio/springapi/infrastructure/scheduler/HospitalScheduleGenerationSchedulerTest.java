package koh.portfolio.springapi.infrastructure.scheduler;

import koh.portfolio.springapi.application.hospital.usecase.GenerateMonthlyHospitalSchedulesUseCase;
import koh.portfolio.springapi.common.time.ServerTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class HospitalScheduleGenerationSchedulerTest {

    private GenerateMonthlyHospitalSchedulesUseCase generateMonthlyHospitalSchedulesUseCase;
    private HospitalScheduleGenerationScheduler scheduler;

    @BeforeEach
    void setUp() {
        generateMonthlyHospitalSchedulesUseCase = mock(GenerateMonthlyHospitalSchedulesUseCase.class);
        scheduler = new HospitalScheduleGenerationScheduler(generateMonthlyHospitalSchedulesUseCase);
    }

    @Test
    @DisplayName("generateNextMonth実行時、JSTの翌月を指定してユースケースを1回だけ呼び出す")
    void generate_next_month_in_jst() {
        // given: 月またぎで揺れないよう、呼び出しの前後で翌月を算出する
        YearMonth nextMonthBefore = YearMonth.now(ServerTime.ZONE_ID).plusMonths(1);

        // when
        scheduler.generateNextMonth();
        YearMonth nextMonthAfter = YearMonth.now(ServerTime.ZONE_ID).plusMonths(1);

        // then
        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(generateMonthlyHospitalSchedulesUseCase, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isIn(nextMonthBefore, nextMonthAfter);
        verifyNoMoreInteractions(generateMonthlyHospitalSchedulesUseCase);
    }

    @Test
    @DisplayName("generateNextMonthは毎月1日0時（JST）にスケジュールされる")
    void scheduled_monthly_at_midnight_jst() throws NoSuchMethodException {
        // when
        Method method = HospitalScheduleGenerationScheduler.class.getMethod("generateNextMonth");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 0 1 * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Tokyo");
    }
}
