package koh.portfolio.springapi.domain.notification.port;

import koh.portfolio.springapi.domain.notification.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findAllByUserId(Long userId);

    // 未読の行だけを既読に更新し、更新できたかを返す。既読済みは0件（冪等）
    boolean markAsRead(Long notificationId, LocalDateTime readAt);
}
