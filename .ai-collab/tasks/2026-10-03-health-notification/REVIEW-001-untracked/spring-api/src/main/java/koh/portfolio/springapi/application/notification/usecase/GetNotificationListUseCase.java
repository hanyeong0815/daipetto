package koh.portfolio.springapi.application.notification.usecase;

import koh.portfolio.springapi.application.notification.dto.NotificationDto.NotificationResponse;

import java.util.List;

public interface GetNotificationListUseCase {
    List<NotificationResponse> execute(Long userId);
}
