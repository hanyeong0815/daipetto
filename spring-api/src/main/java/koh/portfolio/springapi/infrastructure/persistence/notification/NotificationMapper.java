package koh.portfolio.springapi.infrastructure.persistence.notification;

import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface NotificationMapper extends DomainEntityMapper<Notification, NotificationEntity> {
}
