package koh.portfolio.springapi.infrastructure.persistence.pet;

import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PetMapperTest {

    private final PetMapper petMapper = Mappers.getMapper(PetMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Pet domain = new Pet(
                1L, 10L, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                now, now, null
        );

        // when
        PetEntity entity = petMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(10L);
        assertThat(entity.getName()).isEqualTo("Momo");
        assertThat(entity.getPetType()).isEqualTo(PetType.CAT);
        assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(entity.getGender()).isEqualTo(PetGender.FEMALE);
        assertThat(entity.getWeight()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PetEntity entity = PetEntity.builder()
                .id(1L)
                .userId(10L)
                .name("Momo")
                .petType(PetType.CAT)
                .birthDate(LocalDate.of(2023, 1, 1))
                .gender(PetGender.FEMALE)
                .weight(new BigDecimal("4.5"))
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(null)
                .build();

        // when
        Pet domain = petMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getUserId()).isEqualTo(10L);
        assertThat(domain.getName()).isEqualTo("Momo");
        assertThat(domain.getPetType()).isEqualTo(PetType.CAT);
        assertThat(domain.getBirthDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(domain.getGender()).isEqualTo(PetGender.FEMALE);
        assertThat(domain.getWeight()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(domain.getCreatedAt()).isEqualTo(now);
        assertThat(domain.getDeletedAt()).isNull();
    }
}
