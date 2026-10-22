package koh.portfolio.springapi.common.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// 応答の日時はJSTのオフセット付きで返す（例: 2026-05-17T10:00:00+09:00、07_API_Design §2-7）。
// DBとドメインはJSTの LocalDateTime のまま扱い、JSONに出す時だけオフセットを付ける
@JsonComponent
public class LocalDateTimeJsonSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.truncatedTo(ChronoUnit.SECONDS)
                .atZone(ServerTime.ZONE_ID)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
