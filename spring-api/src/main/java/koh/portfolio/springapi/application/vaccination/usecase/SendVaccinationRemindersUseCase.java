package koh.portfolio.springapi.application.vaccination.usecase;

import java.time.LocalDate;

// 接種予定日が近い SCHEDULED の予定に VACCINATION 通知を生成する（08_State_Design §11）。
// 公開APIは持たず、Scheduler から呼ばれる。生成した通知の件数を返す
public interface SendVaccinationRemindersUseCase {
    int execute(LocalDate today);
}
