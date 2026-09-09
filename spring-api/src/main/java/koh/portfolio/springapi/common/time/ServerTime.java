package koh.portfolio.springapi.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public final class ServerTime {
    public final ZoneId zoneId;

    public ServerTime() {
        zoneId = ZoneId.of("Asia/Seoul");
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
