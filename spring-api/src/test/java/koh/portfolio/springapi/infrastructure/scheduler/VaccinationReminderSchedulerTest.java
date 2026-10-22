package koh.portfolio.springapi.infrastructure.scheduler;

import koh.portfolio.springapi.application.vaccination.usecase.SendVaccinationRemindersUseCase;
import koh.portfolio.springapi.common.time.ServerTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class VaccinationReminderSchedulerTest {

    private SendVaccinationRemindersUseCase sendVaccinationRemindersUseCase;
    private VaccinationReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        sendVaccinationRemindersUseCase = mock(SendVaccinationRemindersUseCase.class);
        scheduler = new VaccinationReminderScheduler(sendVaccinationRemindersUseCase);
    }

    @Test
    @DisplayName("remind実行時、JSTの当日を指定してユースケースを1回だけ呼び出す")
    void remind_with_today_in_jst() {
        // given: 日付またぎで揺れないよう、呼び出しの前後で当日を算出する
        LocalDate todayBefore = LocalDate.now(ServerTime.ZONE_ID);

        // when
        scheduler.remind();
        LocalDate todayAfter = LocalDate.now(ServerTime.ZONE_ID);

        // then
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(sendVaccinationRemindersUseCase, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isIn(todayBefore, todayAfter);
        verifyNoMoreInteractions(sendVaccinationRemindersUseCase);
    }

    @Test
    @DisplayName("remindは毎日9時（JST）にスケジュールされる")
    void scheduled_daily_at_nine_jst() throws NoSuchMethodException {
        // when
        Method method = VaccinationReminderScheduler.class.getMethod("remind");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 9 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Tokyo");
    }
}
