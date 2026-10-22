package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VaccinationMapperTest {

    private final VaccinationMapper vaccinationMapper = Mappers.getMapper(VaccinationMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Vaccination domain = new Vaccination(1L, 10L, "混合ワクチン", LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10), VaccinationStatus.COMPLETED, now, now, null);

        // when
        VaccinationEntity entity = vaccinationMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getPetId()).isEqualTo(10L);
        assertThat(entity.getVaccineName()).isEqualTo("混合ワクチン");
        assertThat(entity.getScheduledDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(entity.getCompletedDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(entity.getRemindedDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(entity.getStatus()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        VaccinationEntity entity = VaccinationEntity.builder()
                .id(1L)
                .petId(10L)
                .vaccineName("狂犬病ワクチン")
                .scheduledDate(LocalDate.of(2026, 10, 10))
                .status(VaccinationStatus.SCHEDULED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        Vaccination domain = vaccinationMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getVaccineName()).isEqualTo("狂犬病ワクチン");
        assertThat(domain.getStatus()).isEqualTo(VaccinationStatus.SCHEDULED);
        assertThat(domain.getCompletedDate()).isNull();
    }
}
