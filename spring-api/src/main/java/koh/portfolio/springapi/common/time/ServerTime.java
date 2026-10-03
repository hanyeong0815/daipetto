package koh.portfolio.springapi.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public final class ServerTime {
    // サービスの基準タイムゾーン（日本向けサービスのためJST）。SpringApiApplication.mainでJVMの既定にも設定する
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Tokyo");

    public final ZoneId zoneId;

    public ServerTime() {
        zoneId = ZONE_ID;
    }

    public Instant nowInstant() {
        return this.now().toInstant();
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(zoneId);
    }

    public LocalDateTime nowLocal() {
        return now().toLocalDateTime();
    }
}
