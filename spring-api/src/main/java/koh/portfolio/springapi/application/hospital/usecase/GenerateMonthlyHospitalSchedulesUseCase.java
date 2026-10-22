package koh.portfolio.springapi.application.hospital.usecase;

import java.time.YearMonth;

public interface GenerateMonthlyHospitalSchedulesUseCase {
    // 営業中の全病院について、指定月の予約枠を生成し、作成した枠の合計を返す
    int execute(YearMonth month);
}
