package koh.portfolio.springapi.infrastructure.client.analysis;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import koh.portfolio.springapi.domain.analysis.port.AnalysisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

// 2xx 以外の応答（3xx を含む）・接続失敗・タイムアウト・読めない応答・必須項目の欠けた応答は、すべて ANALYSIS-003（07 §7-5）。
// 必須項目は domain/analysis/model の各 record の生成時に検査する
@Component
@RequiredArgsConstructor
public class AnalysisApiClient implements AnalysisClient {
    static final String KEY_HEADER = "X-Internal-Api-Key";
    private static final String PET_PATH = "/internal/v1/analysis/pets/{petId}";

    private final RestClient analysisRestClient;

    @Override
    public WeightAnalysis getWeight(Long petId, LocalDate from, LocalDate to) {
        return get(uri -> uri.path(PET_PATH + "/weight")
                        .queryParamIfPresent("from", Optional.ofNullable(from))
                        .queryParamIfPresent("to", Optional.ofNullable(to))
                        .build(petId),
                new ParameterizedTypeReference<Envelope<WeightAnalysis>>() {
                });
    }

    @Override
    public HealthScore getHealthScore(Long petId) {
        return get(uri -> uri.path(PET_PATH + "/health-score").build(petId),
                new ParameterizedTypeReference<Envelope<HealthScore>>() {
                });
    }

    @Override
    public VaccinationForecast getVaccinationForecast(Long petId) {
        return get(uri -> uri.path(PET_PATH + "/vaccination/next").build(petId),
                new ParameterizedTypeReference<Envelope<VaccinationForecast>>() {
                });
    }

    private <T> T get(Function<UriBuilder, URI> uri, ParameterizedTypeReference<Envelope<T>> type) {
        Envelope<T> body;
        try {
            body = analysisRestClient.get().uri(uri).retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw AnalysisErrorCode.ANALYSIS_UNAVAILABLE.defaultException();
                    })
                    .body(type);
        } catch (RestClientException e) {
            throw AnalysisErrorCode.ANALYSIS_UNAVAILABLE.defaultException(e);
        }

        Preconditions.validate(body != null && body.success() && body.data() != null,
                AnalysisErrorCode.ANALYSIS_UNAVAILABLE);
        return body.data();
    }

    record Envelope<T>(boolean success, T data) {
    }
}
