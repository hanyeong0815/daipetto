package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesResponse;
import koh.portfolio.springapi.application.hospital.usecase.GenerateHospitalSchedulesUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GenerateMonthlyHospitalSchedulesUseCase;
import koh.portfolio.springapi.common.time.ServerTime;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalScheduleGenerationService implements GenerateHospitalSchedulesUseCase, GenerateMonthlyHospitalSchedulesUseCase {
    static final int MAX_MONTHS_AHEAD = 12;

    private final HospitalAccessChecker hospitalAccessChecker;
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleGenerator hospitalScheduleGenerator;

    // 管理画面からの手動生成。今月〜12か月先の月を指定でき、今月は今日（JST）以降の日だけを作る
    @Override
    public GenerateHospitalSchedulesResponse execute(Long userId, Long hospitalId, YearMonth month) {
        hospitalAccessChecker.checkManageable(userId, hospitalId);

        LocalDate today = LocalDate.now(ServerTime.ZONE_ID);
        YearMonth currentMonth = YearMonth.from(today);
        validate(
                !month.isBefore(currentMonth) && !month.isAfter(currentMonth.plusMonths(MAX_MONTHS_AHEAD)),
                HospitalErrorCode.INVALID_GENERATION_MONTH
        );

        LocalDate from = month.equals(currentMonth) ? today : month.atDay(1);
        return hospitalScheduleGenerator.generate(hospitalId, from, month.atEndOfMonth());
    }

    // 月次バッチ。営業中の全病院が対象で、1病院の失敗で他の病院の生成を止めない（病院ごとに別トランザクション）
    @Override
    public int execute(YearMonth month) {
        int created = 0;
        for (Hospital hospital : hospitalRepository.findAllForManagement()) {
            if (hospital.getStatus() != HospitalStatus.ACTIVE) {
                continue;
            }
            try {
                created += hospitalScheduleGenerator.generate(hospital.getId(), month.atDay(1), month.atEndOfMonth()).createdSlots();
            } catch (RuntimeException exception) {
                log.error("予約枠の自動生成に失敗しました。hospitalId={}, month={}", hospital.getId(), month, exception);
            }
        }
        return created;
    }
}
