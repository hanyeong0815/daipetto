package koh.portfolio.springapi.domain.analysis.model;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

// 次回接種推奨日（07 §7-4）。source: SCHEDULED（登録済みの予定）/ RECOMMENDED（最後の接種日の1年後）。
// lastCompletedDate・vaccinationId だけが null 可
public record VaccinationForecast(List<Item> items) {
    public VaccinationForecast {
        items = List.copyOf(items);
    }

    public record Item(String vaccineName, LocalDate lastCompletedDate, LocalDate nextDate, String source,
                       Long vaccinationId, Boolean overdue, Long daysUntil) {
        public Item {
            requireNonNull(vaccineName);
            requireNonNull(nextDate);
            requireNonNull(source);
            requireNonNull(overdue);
            requireNonNull(daysUntil);
        }
    }
}
