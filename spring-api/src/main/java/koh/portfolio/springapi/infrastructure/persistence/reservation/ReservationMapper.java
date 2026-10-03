package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface ReservationMapper extends DomainEntityMapper<Reservation, ReservationEntity> {
}
