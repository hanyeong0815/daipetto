package koh.portfolio.springapi.infrastructure.client.analysis;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnalysisApiClientTest {

    private static final String BASE = "http://analysis.test/internal/v1/analysis/pets/7";

    private MockRestServiceServer server;
    private AnalysisApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://analysis.test")
                .defaultHeader(AnalysisApiClient.KEY_HEADER, "test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnalysisApiClient(builder.build());
    }

    private void respond(String url, String json) {
        server.expect(requestTo(url))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(AnalysisApiClient.KEY_HEADER, "test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void assertUnavailable(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(AnalysisErrorCode.ANALYSIS_UNAVAILABLE.code()));
    }

    @Test
    @DisplayName("体重推移: 内部APIキーと期間を付けて呼び、data を読み取る")
    void get_weight_with_period() {
        respond(BASE + "/weight?from=2026-10-01&to=2026-10-25", """
                {"success": true, "data": {
                  "points": [{"date": "2026-10-01", "weight": 4.3}, {"date": "2026-10-20", "weight": 4.52}],
                  "summary": {"count": 2, "first": 4.3, "latest": 4.52, "min": 4.3, "max": 4.52,
                              "change": 0.22, "changeRate": 5.1, "unknownField": 1}}}
                """);

        WeightAnalysis analysis = client.getWeight(7L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25));

        assertThat(analysis.points()).extracting(WeightAnalysis.Point::date)
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20));
        assertThat(analysis.summary().latest()).isEqualByComparingTo("4.52");
        assertThat(analysis.summary().changeRate()).isEqualByComparingTo("5.1");
        server.verify();
    }

    @Test
    @DisplayName("体重推移: 期間を省略するとクエリを付けない")
    void get_weight_without_period() {
        respond(BASE + "/weight", "{\"success\": true, \"data\": {\"points\": [], \"summary\": null}}");

        WeightAnalysis analysis = client.getWeight(7L, null, null);

        assertThat(analysis.points()).isEmpty();
        assertThat(analysis.summary()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("健康スコア: NO_DATA（score null）を読み取る")
    void get_health_score_no_data() {
        respond(BASE + "/health-score", """
                {"success": true, "data": {"score": null, "grade": "NO_DATA",
                  "period": {"from": "2026-09-26", "to": "2026-10-25"},
                  "factors": [{"type": "WEIGHT_CHANGE", "evaluated": false, "penalty": 0, "value": null},
                              {"type": "SYMPTOM_FREQUENCY", "evaluated": true, "penalty": 10, "value": 1}]}}
                """);

        HealthScore score = client.getHealthScore(7L);

        assertThat(score.score()).isNull();
        assertThat(score.grade()).isEqualTo("NO_DATA");
        assertThat(score.period().from()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(score.factors().get(1).value()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("次回接種推奨日: 予定のIDと日数を読み取る")
    void get_vaccination_forecast() {
        respond(BASE + "/vaccination/next", """
                {"success": true, "data": {"items": [{"vaccineName": "混合ワクチン", "lastCompletedDate": null,
                  "nextDate": "2026-11-20", "source": "SCHEDULED", "vaccinationId": 4, "overdue": false, "daysUntil": 26}]}}
                """);

        VaccinationForecast forecast = client.getVaccinationForecast(7L);

        assertThat(forecast.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.vaccinationId()).isEqualTo(4L);
                    assertThat(item.lastCompletedDate()).isNull();
                    assertThat(item.daysUntil()).isEqualTo(26L);
                });
    }

    @Test
    @DisplayName("分析APIが401（キー不一致）・500を返したらANALYSIS-003")
    void error_status_becomes_unavailable() {
        server.expect(requestTo(BASE + "/health-score")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertUnavailable(() -> client.getHealthScore(7L));

        server.reset();
        server.expect(requestTo(BASE + "/health-score")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertUnavailable(() -> client.getHealthScore(7L));
    }

    @Test
    @DisplayName("3xx（302）は成功の形の本文でもANALYSIS-003")
    void redirect_status_becomes_unavailable() {
        server.expect(requestTo(BASE + "/weight"))
                .andRespond(withStatus(HttpStatus.FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\": true, \"data\": {\"points\": [], \"summary\": null}}"));

        assertUnavailable(() -> client.getWeight(7L, null, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteBodies")
    @DisplayName("必須項目が欠けた・nullの応答はANALYSIS-003")
    void incomplete_body_becomes_unavailable(String label, String path, String data) {
        server.expect(requestTo(BASE + path))
                .andRespond(withSuccess("{\"success\": true, \"data\": " + data + "}", MediaType.APPLICATION_JSON));

        assertUnavailable(() -> {
            switch (path) {
                case "/weight" -> client.getWeight(7L, null, null);
                case "/health-score" -> client.getHealthScore(7L);
                default -> client.getVaccinationForecast(7L);
            }
        });
    }

    static Stream<Arguments> incompleteBodies() {
        String factor = "{\"type\": \"WEIGHT_CHANGE\", \"evaluated\": true, \"penalty\": 15, \"value\": 6.2}";
        String period = "{\"from\": \"2026-09-26\", \"to\": \"2026-10-25\"}";
        String item = "\"vaccineName\": \"A\", \"lastCompletedDate\": null, \"source\": \"RECOMMENDED\", \"vaccinationId\": null";
        return Stream.of(
                Arguments.of("weight: data が空", "/weight", "{}"),
                Arguments.of("weight: points が null", "/weight", "{\"points\": null, \"summary\": null}"),
                Arguments.of("weight: points に null", "/weight", "{\"points\": [null], \"summary\": null}"),
                Arguments.of("weight: point の weight なし", "/weight", "{\"points\": [{\"date\": \"2026-10-01\"}], \"summary\": null}"),
                Arguments.of("weight: summary の count なし", "/weight",
                        "{\"points\": [], \"summary\": {\"first\": 4, \"latest\": 4, \"min\": 4, \"max\": 4, \"change\": 0, \"changeRate\": 0}}"),
                Arguments.of("health-score: grade なし", "/health-score",
                        "{\"score\": 100, \"period\": " + period + ", \"factors\": []}"),
                Arguments.of("health-score: period なし", "/health-score", "{\"score\": 100, \"grade\": \"GOOD\", \"factors\": []}"),
                Arguments.of("health-score: factors なし", "/health-score",
                        "{\"score\": 100, \"grade\": \"GOOD\", \"period\": " + period + "}"),
                Arguments.of("health-score: factor の penalty なし", "/health-score",
                        "{\"score\": 100, \"grade\": \"GOOD\", \"period\": " + period + ", \"factors\": ["
                                + factor.replace(", \"penalty\": 15", "") + "]}"),
                Arguments.of("vaccination: items なし", "/vaccination/next", "{}"),
                Arguments.of("vaccination: nextDate なし", "/vaccination/next",
                        "{\"items\": [{" + item + ", \"overdue\": false, \"daysUntil\": 1}]}"),
                Arguments.of("vaccination: overdue が null", "/vaccination/next",
                        "{\"items\": [{" + item + ", \"nextDate\": \"2026-11-01\", \"overdue\": null, \"daysUntil\": 1}]}")
        );
    }

    @Test
    @DisplayName("接続失敗・タイムアウトはANALYSIS-003")
    void io_error_becomes_unavailable() {
        server.expect(requestTo(BASE + "/vaccination/next")).andRespond(request -> {
            throw new IOException("connection refused");
        });

        assertUnavailable(() -> client.getVaccinationForecast(7L));
    }

    @Test
    @DisplayName("読めない応答・data の無い応答はANALYSIS-003")
    void unreadable_body_becomes_unavailable() {
        server.expect(requestTo(BASE + "/weight")).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertUnavailable(() -> client.getWeight(7L, null, null));

        server.reset();
        server.expect(requestTo(BASE + "/weight"))
                .andRespond(withSuccess("{\"success\": false, \"data\": null}", MediaType.APPLICATION_JSON));
        assertUnavailable(() -> client.getWeight(7L, null, null));
    }
}
