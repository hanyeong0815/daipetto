package koh.portfolio.springapi.domain.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

// 健康スコア（07 §7-2）。grade: GOOD / CAUTION / WARNING / NO_DATA、factor type: WEIGHT_CHANGE / SYMPTOM_FREQUENCY。
// score（NO_DATA のとき）と factor の value（評価しないとき）だけが null 可
public record HealthScore(Integer score, String grade, Period period, List<Factor> factors) {
    public HealthScore {
        requireNonNull(grade);
        requireNonNull(period);
        factors = List.copyOf(factors);
    }

    public record Period(LocalDate from, LocalDate to) {
        public Period {
            requireNonNull(from);
            requireNonNull(to);
        }
    }

    public record Factor(String type, Boolean evaluated, Integer penalty, BigDecimal value) {
        public Factor {
            requireNonNull(type);
            requireNonNull(evaluated);
            requireNonNull(penalty);
        }
    }
}
