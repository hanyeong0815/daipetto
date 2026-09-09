package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalMapperTest {

    private final HospitalMapper hospitalMapper = Mappers.getMapper(HospitalMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Hospital domain = new Hospital(1L, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678",
                HospitalStatus.ACTIVE, now, now, null);

        // when
        HospitalEntity entity = hospitalMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Tokyo Animal Hospital");
        assertThat(entity.getAddress()).isEqualTo("Tokyo, Shibuya");
        assertThat(entity.getPhoneNumber()).isEqualTo("03-1234-5678");
        assertThat(entity.getStatus()).isEqualTo(HospitalStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalEntity entity = HospitalEntity.builder()
                .id(1L)
                .name("Tokyo Animal Hospital")
                .address("Tokyo, Shibuya")
                .phoneNumber("03-1234-5678")
                .status(HospitalStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(null)
                .build();

        // when
        Hospital domain = hospitalMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getName()).isEqualTo("Tokyo Animal Hospital");
        assertThat(domain.getAddress()).isEqualTo("Tokyo, Shibuya");
        assertThat(domain.getPhoneNumber()).isEqualTo("03-1234-5678");
        assertThat(domain.getStatus()).isEqualTo(HospitalStatus.ACTIVE);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
        assertThat(domain.getDeletedAt()).isNull();
    }
}
