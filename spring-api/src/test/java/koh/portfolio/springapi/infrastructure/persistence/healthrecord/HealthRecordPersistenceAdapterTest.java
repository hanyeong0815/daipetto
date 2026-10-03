package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
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
        HealthRecordPersistenceAdapter.class,
        HealthRecordMapperImpl.class
})
class HealthRecordPersistenceAdapterTest {

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private HealthRecordJpaRepository healthRecordJpaRepository;

    private HealthRecordEntity persist(LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        return healthRecordJpaRepository.save(HealthRecordEntity.builder()
                .petId(10L)
                .weight(new BigDecimal("4.70"))
                .symptom("cough")
                .memo("original")
                .recordedDate(LocalDate.of(2026, 5, 20))
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(deletedAt)
                .build());
    }

    private HealthRecord patched(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new HealthRecord(id, 10L, new BigDecimal("5.20"), "better", "racing",
                LocalDate.of(2026, 5, 21), now, now, null);
    }

    @Test
    @DisplayName("論理削除されていない記録は内容列が更新される")
    void update_content_writes_active_row() {
        // given
        HealthRecordEntity saved = persist(null);

        // when
        boolean updated = healthRecordRepository.updateContent(patched(saved.getId()));

        // then
        assertThat(updated).isTrue();
        HealthRecordEntity reloaded = healthRecordJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getWeight()).isEqualByComparingTo("5.20");
        assertThat(reloaded.getMemo()).isEqualTo("racing");
        assertThat(reloaded.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("論理削除済みの記録は更新されず、deleted_atも維持される（REVIEW-001 R-02）")
    void update_content_does_not_resurrect_deleted_row() {
        // given
        LocalDateTime deletedAt = LocalDateTime.of(2026, 10, 3, 12, 0);
        HealthRecordEntity saved = persist(deletedAt);

        // when: 削除前に読み取った（deleted_at=NULLの）内容で書き込もうとする
        boolean updated = healthRecordRepository.updateContent(patched(saved.getId()));

        // then
        assertThat(updated).isFalse();
        HealthRecordEntity reloaded = healthRecordJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(reloaded.getMemo()).isEqualTo("original");
    }

    @Test
    @DisplayName("排他取得は論理削除されていない記録だけを返す（REVIEW-002 R-04）")
    void find_by_id_for_update_returns_only_active_row() {
        // given
        HealthRecordEntity active = persist(null);
        HealthRecordEntity deleted = persist(LocalDateTime.of(2026, 10, 3, 12, 0));

        // when & then
        assertThat(healthRecordRepository.findByIdForUpdate(active.getId()))
                .hasValueSatisfying(h -> assertThat(h.getMemo()).isEqualTo("original"));
        assertThat(healthRecordRepository.findByIdForUpdate(deleted.getId())).isEmpty();
    }
}
