package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        HospitalPersistenceAdapter.class,
        HospitalMapperImpl.class
})
class HospitalPersistenceAdapterTest {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalJpaRepository hospitalJpaRepository;

    private HospitalEntity persist(HospitalStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return hospitalJpaRepository.save(HospitalEntity.builder()
                .name("Tokyo Animal Hospital")
                .address("Tokyo, Shibuya")
                .phoneNumber("03-1234-5678")
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Test
    @DisplayName("情報更新はstatusを書き込まない。停止済みの病院がACTIVEに戻らない（REVIEW-001 R-03）")
    void update_info_keeps_status() {
        // given: 停止がcommitされた後の行
        HospitalEntity saved = persist(HospitalStatus.SUSPENDED);
        LocalDateTime now = LocalDateTime.now();

        // when: 停止前に読み取った（ACTIVEの）病院で情報更新する
        Hospital staleActive = new Hospital(saved.getId(), "Renamed Hospital", "Tokyo, Shinjuku", "03-9999-9999",
                HospitalStatus.ACTIVE, now, now, null);
        boolean updated = hospitalRepository.updateInfo(staleActive);

        // then
        assertThat(updated).isTrue();
        HospitalEntity reloaded = hospitalJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(HospitalStatus.SUSPENDED);
        assertThat(reloaded.getName()).isEqualTo("Renamed Hospital");
        assertThat(reloaded.getAddress()).isEqualTo("Tokyo, Shinjuku");
    }

    @Test
    @DisplayName("停止はstatusだけを書き込み、同時に更新された情報を古い値で上書きしない")
    void update_status_keeps_info() {
        // given
        HospitalEntity saved = persist(HospitalStatus.ACTIVE);
        hospitalRepository.updateInfo(new Hospital(saved.getId(), "Renamed Hospital", "Tokyo, Shinjuku", null,
                HospitalStatus.ACTIVE, saved.getCreatedAt(), LocalDateTime.now(), null));

        // when
        boolean updated = hospitalRepository.updateStatus(saved.getId(), HospitalStatus.SUSPENDED, LocalDateTime.now());

        // then
        assertThat(updated).isTrue();
        HospitalEntity reloaded = hospitalJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(HospitalStatus.SUSPENDED);
        assertThat(reloaded.getName()).isEqualTo("Renamed Hospital");
    }

    @Test
    @DisplayName("存在しない病院の更新は0件でfalseを返却する")
    void updates_report_missing_row() {
        LocalDateTime now = LocalDateTime.now();

        assertThat(hospitalRepository.updateStatus(999_999L, HospitalStatus.SUSPENDED, now)).isFalse();
        assertThat(hospitalRepository.updateInfo(new Hospital(999_999L, "x", "y", null,
                HospitalStatus.ACTIVE, now, now, null))).isFalse();
    }
}
