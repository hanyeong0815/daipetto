package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest {

    private final ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Reservation domain = new Reservation(1L, 10L, 100L, 1000L, 10000L, now, ReservationStatus.REQUESTED, "咳があります", now, now, null);

        // when
        ReservationEntity entity = reservationMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(10L);
        assertThat(entity.getPetId()).isEqualTo(100L);
        assertThat(entity.getHospitalId()).isEqualTo(1000L);
        assertThat(entity.getScheduleId()).isEqualTo(10000L);
        assertThat(entity.getReservationDatetime()).isEqualTo(now);
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(entity.getMemo()).isEqualTo("咳があります");
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ReservationEntity entity = ReservationEntity.builder()
                .id(1L)
                .userId(10L)
                .petId(100L)
                .hospitalId(1000L)
                .scheduleId(10000L)
                .reservationDatetime(now)
                .status(ReservationStatus.REQUESTED)
                .memo("咳があります")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        Reservation domain = reservationMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getUserId()).isEqualTo(10L);
        assertThat(domain.getPetId()).isEqualTo(100L);
        assertThat(domain.getHospitalId()).isEqualTo(1000L);
        assertThat(domain.getScheduleId()).isEqualTo(10000L);
        assertThat(domain.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(domain.getMemo()).isEqualTo("咳があります");
    }
}
