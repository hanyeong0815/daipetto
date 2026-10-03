package koh.portfolio.springapi.infrastructure.persistence.notification;

import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepository {
    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity savedEntity = notificationJpaRepository.save(notificationMapper.toEntity(notification));

        return notificationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id)
                .map(notificationMapper::toDomain);
    }

    @Override
    public List<Notification> findAllByUserId(Long userId) {
        return notificationJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(notificationMapper::toDomain).toList();
    }

    @Override
    public boolean markAsRead(Long notificationId, LocalDateTime readAt) {
        return notificationJpaRepository.markAsRead(notificationId, readAt) == 1;
    }
}
