package koh.portfolio.springapi.domain.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

// 体重推移（07 §7-1）。分析API（FastAPI）が算出した値をそのまま保持する。
// 必須項目が欠けた応答は生成時に拒否する（分析APIの応答では null 不可。summary・changeRate のみ null 可）
public record WeightAnalysis(List<Point> points, Summary summary) {
    public WeightAnalysis {
        points = List.copyOf(points);
    }

    public record Point(LocalDate date, BigDecimal weight) {
        public Point {
            requireNonNull(date);
            requireNonNull(weight);
        }
    }

    public record Summary(Integer count, BigDecimal first, BigDecimal latest, BigDecimal min, BigDecimal max,
                          BigDecimal change, BigDecimal changeRate) {
        public Summary {
            requireNonNull(count);
            requireNonNull(first);
            requireNonNull(latest);
            requireNonNull(min);
            requireNonNull(max);
            requireNonNull(change);
        }
    }
}
