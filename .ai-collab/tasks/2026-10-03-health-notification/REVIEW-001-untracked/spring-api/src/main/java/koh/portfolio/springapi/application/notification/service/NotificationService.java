package koh.portfolio.springapi.application.notification.service;

import koh.portfolio.springapi.application.notification.dto.NotificationDto.NotificationResponse;
import koh.portfolio.springapi.application.notification.usecase.GetNotificationListUseCase;
import koh.portfolio.springapi.application.notification.usecase.MarkNotificationAsReadUseCase;
import koh.portfolio.springapi.application.notification.usecase.NotifyReservationEventUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.notification.exception.NotificationErrorCode;
import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import koh.portfolio.springapi.domain.notification.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService implements
        GetNotificationListUseCase,
        MarkNotificationAsReadUseCase,
        NotifyReservationEventUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationResponse> execute(Long userId) {
        return notificationRepository.findAllByUserId(userId).stream()
                .map(notification -> NotificationResponse.builder()
                        .notificationId(notification.getId())
                        .type(notification.getNotificationType())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .isRead(notification.isRead())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void execute(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationErrorCode.NOTIFICATION_NOT_FOUND::defaultException);

        Preconditions.validate(
                notification.getUserId().equals(userId),
                NotificationErrorCode.NOT_NOTIFICATION_OWNER
        );

        // 既読済みなら0件更新。再実行しても成功扱いにする（冪等）
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void execute(Long userId, Long reservationId, NotificationType notificationType) {
        notificationRepository.save(Notification.forReservation(userId, reservationId, notificationType));
    }
}
