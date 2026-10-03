package koh.portfolio.springapi;

import koh.portfolio.springapi.common.time.ServerTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SpringApiApplication {

    public static void main(String[] args) {
        // LocalDate.now()などの既定タイムゾーンを実行環境（UTCのコンテナ等）に依存させず、JSTに固定する
        TimeZone.setDefault(TimeZone.getTimeZone(ServerTime.ZONE_ID));
        SpringApplication.run(SpringApiApplication.class, args);
    }

}
