package koh.portfolio.springapi.infrastructure.persistence.pet;

import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        PetPersistenceAdapter.class,
        PetMapperImpl.class
})
class PetPersistenceAdapterTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetJpaRepository petJpaRepository;

    private PetEntity persist(LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        return petJpaRepository.save(PetEntity.builder()
                .userId(1L)
                .name("Momo")
                .petType(PetType.CAT)
                .birthDate(LocalDate.of(2023, 1, 1))
                .gender(PetGender.FEMALE)
                .weight(new BigDecimal("4.50"))
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(deletedAt)
                .build());
    }

    private Pet renamed(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new Pet(id, 1L, "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE,
                new BigDecimal("5.00"), now, now, null);
    }

    @Test
    @DisplayName("論理削除されていないペットはプロフィール列が更新される")
    void update_profile_writes_active_row() {
        // given
        PetEntity saved = persist(null);

        // when
        boolean updated = petRepository.updateProfile(renamed(saved.getId()));

        // then
        assertThat(updated).isTrue();
        assertThat(petJpaRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("MomoUpdated");
    }

    @Test
    @DisplayName("論理削除済みのペットは更新されず、削除状態が維持される")
    void update_profile_does_not_resurrect_deleted_row() {
        // given
        LocalDateTime deletedAt = LocalDateTime.of(2026, 10, 3, 12, 0);
        PetEntity saved = persist(deletedAt);

        // when
        boolean updated = petRepository.updateProfile(renamed(saved.getId()));

        // then
        assertThat(updated).isFalse();
        PetEntity reloaded = petJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(reloaded.getName()).isEqualTo("Momo");
    }
}
