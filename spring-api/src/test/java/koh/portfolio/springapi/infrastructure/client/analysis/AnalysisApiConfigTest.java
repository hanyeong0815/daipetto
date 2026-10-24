package koh.portfolio.springapi.infrastructure.client.analysis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 実際のHTTP接続で、設定したクライアント（AnalysisApiConfig）がキーを送り、リダイレクトを追わないことを確認する
class AnalysisApiConfigTest {

    private static final String VALID = "{\"success\": true, \"data\": {\"points\": [], \"summary\": null}}";

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> keys = new CopyOnWriteArrayList<>();
    private AnalysisApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().getPath());
            keys.add(exchange.getRequestHeaders().getFirst(AnalysisApiClient.KEY_HEADER));
            switch (exchange.getRequestURI().getPath()) {
                case "/internal/v1/analysis/pets/1/weight" -> respond(exchange, 200, VALID);
                case "/internal/v1/analysis/pets/2/weight" -> {
                    exchange.getResponseHeaders().add("Location", "/redirected");
                    respond(exchange, 302, VALID);
                }
                default -> respond(exchange, 200, VALID);
            }
        });
        server.start();

        RestClient restClient = new AnalysisApiConfig().analysisRestClient(
                RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort(), "config-test-key");
        client = new AnalysisApiClient(restClient);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    @DisplayName("200の応答を読み取り、内部APIキーを送る")
    void reads_success_and_sends_key() {
        WeightAnalysis analysis = client.getWeight(1L, null, null);

        assertThat(analysis.points()).isEmpty();
        assertThat(keys).containsExactly("config-test-key");
    }

    @Test
    @DisplayName("302はリダイレクト先を呼ばずにANALYSIS-003")
    void does_not_follow_redirect() {
        assertThatThrownBy(() -> client.getWeight(2L, null, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(AnalysisErrorCode.ANALYSIS_UNAVAILABLE.code()));
        assertThat(requests).containsExactly("/internal/v1/analysis/pets/2/weight");
    }
}
