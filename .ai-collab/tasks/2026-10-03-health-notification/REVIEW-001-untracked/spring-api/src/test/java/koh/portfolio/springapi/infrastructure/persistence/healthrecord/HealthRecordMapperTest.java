package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HealthRecordMapperTest {

    private final HealthRecordMapper healthRecordMapper = Mappers.getMapper(HealthRecordMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HealthRecord domain = new HealthRecord(1L, 10L, new BigDecimal("4.70"), "咳", "少し元気がない",
                LocalDate.of(2026, 5, 20), now, now, null);

        // when
        HealthRecordEntity entity = healthRecordMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getPetId()).isEqualTo(10L);
        assertThat(entity.getWeight()).isEqualByComparingTo("4.70");
        assertThat(entity.getSymptom()).isEqualTo("咳");
        assertThat(entity.getMemo()).isEqualTo("少し元気がない");
        assertThat(entity.getRecordedDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HealthRecordEntity entity = HealthRecordEntity.builder()
                .id(1L)
                .petId(10L)
                .weight(new BigDecimal("4.70"))
                .symptom("咳")
                .memo("少し元気がない")
                .recordedDate(LocalDate.of(2026, 5, 20))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        HealthRecord domain = healthRecordMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getPetId()).isEqualTo(10L);
        assertThat(domain.getWeight()).isEqualByComparingTo("4.70");
        assertThat(domain.getRecordedDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }
}
