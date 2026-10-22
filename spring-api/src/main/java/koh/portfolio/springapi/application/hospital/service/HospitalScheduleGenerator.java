package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesResponse;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

// 1病院・1期間の予約枠を曜日別営業時間から生成する（06_ERD §18）。月次バッチで病院ごとに別トランザクションにするため別Bean
@Component
@RequiredArgsConstructor
public class HospitalScheduleGenerator {
    private final HospitalRepository hospitalRepository;
    private final HospitalBusinessHoursRepository hospitalBusinessHoursRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Transactional
    public GenerateHospitalSchedulesResponse generate(Long hospitalId, LocalDate from, LocalDate to) {
        // 同じ病院の生成（月次バッチと手動、手動の同時実行）を直列化する。これが無いと、
        // 両方が「枠のない日」と判定してから保存し、同じ日に枠が二重にできる
        validate(hospitalRepository.findByIdForUpdate(hospitalId).isPresent(), HospitalErrorCode.HOSPITAL_NOT_FOUND);

        Map<DayOfWeek, HospitalBusinessHours> hoursByDay = hospitalBusinessHoursRepository.findByHospitalId(hospitalId).stream()
                .collect(Collectors.toMap(HospitalBusinessHours::getDayOfWeek, Function.identity()));
        Set<LocalDate> datesWithSchedules = new HashSet<>(hospitalScheduleRepository.findDatesWithSchedules(hospitalId, from, to));

        List<HospitalSchedule> slots = new ArrayList<>();
        int generatedDates = 0;
        int skippedDates = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            HospitalBusinessHours hours = hoursByDay.get(date.getDayOfWeek());
            if (hours == null) {
                continue; // 営業時間の無い曜日は休診
            }
            if (datesWithSchedules.contains(date)) {
                skippedDates++; // 手動の枠や臨時のBLOCKEDを上書きしないよう、枠が1つでもある日は作らない
                continue;
            }
            List<HospitalSchedule> daySlots = hours.slotsOn(date);
            if (!daySlots.isEmpty()) {
                slots.addAll(daySlots);
                generatedDates++;
            }
        }

        hospitalScheduleRepository.saveAll(slots);
        return new GenerateHospitalSchedulesResponse(slots.size(), generatedDates, skippedDates);
    }
}
