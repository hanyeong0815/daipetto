package koh.portfolio.springapi.infrastructure.client.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;

@Configuration
public class AnalysisApiConfig {

    // 分析API（FastAPI）の内部APIを呼ぶクライアント。キーは分析API側の INTERNAL_API_KEY と同じ値（07 §7-5）
    @Bean
    public RestClient analysisRestClient(
            RestClient.Builder builder,
            @Value("${analysis-api.url}") String url,
            @Value("${analysis-api.internal-api-key}") String internalApiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // リダイレクトは追わない。元の応答が 2xx でなければ ANALYSIS-003（AnalysisApiClient）
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(url)
                .defaultHeader(AnalysisApiClient.KEY_HEADER, internalApiKey)
                .requestFactory(requestFactory)
                .build();
    }
}
