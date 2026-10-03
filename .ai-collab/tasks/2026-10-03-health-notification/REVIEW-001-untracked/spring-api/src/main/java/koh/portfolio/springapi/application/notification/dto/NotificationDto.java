package koh.portfolio.springapi.application.notification.dto;

import koh.portfolio.springapi.domain.notification.model.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

public record NotificationDto() {
    @Builder
    public record NotificationResponse(
            Long notificationId,
            NotificationType type,
            String title,
            String message,
            boolean isRead,
            LocalDateTime createdAt
    ) {}
}
