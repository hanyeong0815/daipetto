package koh.portfolio.springapi.application.notification.usecase;

public interface MarkNotificationAsReadUseCase {
    void execute(Long userId, Long notificationId);
}
