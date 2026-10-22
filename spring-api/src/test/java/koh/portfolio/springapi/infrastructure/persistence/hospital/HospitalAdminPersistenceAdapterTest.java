package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdminSummary;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.infrastructure.persistence.user.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        HospitalAdminPersistenceAdapter.class,
        HospitalAdminMapperImpl.class
})
class HospitalAdminPersistenceAdapterTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 20, 9, 0);

    @Autowired
    private HospitalAdminRepository hospitalAdminRepository;

    @Autowired
    private TestEntityManager em;

    private Long user(String nickname) {
        return em.persist(UserEntity.builder().email(nickname + "@example.com").password("pw").nickname(nickname)
                .role(Role.ROLE_HOSPITAL_ADMIN).status(UserStatus.ACTIVE).createdAt(AT).updatedAt(AT).build()).getId();
    }

    @Test
    @DisplayName("紐付けの登録・存在確認・病院ごとの管理者（ユーザー結合）・ユーザーごとの病院ID・解除")
    void link_query_and_unlink() {
        // given
        Long vet = user("Vet");
        Long nurse = user("Nurse");
        Long gone = em.persist(UserEntity.builder().email("gone@example.com").password("pw").nickname("Gone")
                .role(Role.ROLE_HOSPITAL_ADMIN).status(UserStatus.ACTIVE).createdAt(AT).updatedAt(AT).deletedAt(AT).build()).getId();
        hospitalAdminRepository.save(HospitalAdmin.link(vet, 1L));
        hospitalAdminRepository.save(HospitalAdmin.link(vet, 3L));
        hospitalAdminRepository.save(HospitalAdmin.link(nurse, 1L));
        hospitalAdminRepository.save(HospitalAdmin.link(gone, 1L)); // 論理削除済みユーザーは一覧に出さない

        // when & then
        assertThat(hospitalAdminRepository.exists(vet, 1L)).isTrue();
        assertThat(hospitalAdminRepository.exists(vet, 2L)).isFalse();
        assertThat(hospitalAdminRepository.findAdminsByHospitalId(1L))
                .extracting(HospitalAdminSummary::nickname).containsExactly("Vet", "Nurse");
        assertThat(hospitalAdminRepository.findHospitalIdsByUserId(vet)).containsExactly(1L, 3L);

        assertThat(hospitalAdminRepository.delete(vet, 1L)).isTrue();
        assertThat(hospitalAdminRepository.delete(vet, 1L)).isFalse();
        assertThat(hospitalAdminRepository.findHospitalIdsByUserId(vet)).containsExactly(3L);

        hospitalAdminRepository.deleteAllByUserId(vet);
        assertThat(hospitalAdminRepository.findHospitalIdsByUserId(vet)).isEmpty();
        assertThat(hospitalAdminRepository.exists(nurse, 1L)).isTrue();
    }

    @Test
    @DisplayName("同じ紐付けの二重登録は一意制約違反をHOSPITAL-007に変換する")
    void save_translates_duplicate_link() {
        // given
        Long vet = user("Vet");
        hospitalAdminRepository.save(HospitalAdmin.link(vet, 1L));

        // when & then
        assertThatThrownBy(() -> hospitalAdminRepository.save(HospitalAdmin.link(vet, 1L)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HospitalErrorCode.HOSPITAL_ADMIN_DUPLICATED.code()));
    }
}
