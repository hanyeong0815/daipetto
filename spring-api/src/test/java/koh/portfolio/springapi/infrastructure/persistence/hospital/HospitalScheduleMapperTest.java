package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalScheduleMapperTest {

    private final HospitalScheduleMapper hospitalScheduleMapper = Mappers.getMapper(HospitalScheduleMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalSchedule domain = new HospitalSchedule(1L, 10L, LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0), LocalTime.of(10, 30), HospitalScheduleStatus.AVAILABLE, now, now);

        // when
        HospitalScheduleEntity entity = hospitalScheduleMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getHospitalId()).isEqualTo(10L);
        assertThat(entity.getAvailableDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(entity.getStatus()).isEqualTo(HospitalScheduleStatus.AVAILABLE);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalScheduleEntity entity = HospitalScheduleEntity.builder()
                .id(1L)
                .hospitalId(10L)
                .availableDate(LocalDate.of(2026, 5, 20))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .status(HospitalScheduleStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        HospitalSchedule domain = hospitalScheduleMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getHospitalId()).isEqualTo(10L);
        assertThat(domain.getAvailableDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(domain.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(domain.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(domain.getStatus()).isEqualTo(HospitalScheduleStatus.AVAILABLE);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }
}
