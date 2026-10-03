package koh.portfolio.springapi.infrastructure.persistence.notification;

import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Notification domain = new Notification(1L, 10L, 100L, null, NotificationType.RESERVATION_APPROVED,
                "予約が承認されました", "ご予約が承認されました。予約内容をご確認ください。", false, now, null);

        // when
        NotificationEntity entity = notificationMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(10L);
        assertThat(entity.getReservationId()).isEqualTo(100L);
        assertThat(entity.getVaccinationId()).isNull();
        assertThat(entity.getNotificationType()).isEqualTo(NotificationType.RESERVATION_APPROVED);
        assertThat(entity.getTitle()).isEqualTo("予約が承認されました");
        assertThat(entity.isRead()).isFalse();
        assertThat(entity.getReadAt()).isNull();
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        NotificationEntity entity = NotificationEntity.builder()
                .id(1L)
                .userId(10L)
                .reservationId(100L)
                .notificationType(NotificationType.TREATMENT_COMPLETED)
                .title("診療が完了しました")
                .message("診療が完了しました。健康記録の登録をおすすめします。")
                .read(true)
                .createdAt(now)
                .readAt(now)
                .build();

        // when
        Notification domain = notificationMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getUserId()).isEqualTo(10L);
        assertThat(domain.getNotificationType()).isEqualTo(NotificationType.TREATMENT_COMPLETED);
        assertThat(domain.isRead()).isTrue();
        assertThat(domain.getReadAt()).isEqualTo(now);
    }
}
